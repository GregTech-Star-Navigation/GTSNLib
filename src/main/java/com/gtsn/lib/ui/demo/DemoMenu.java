package com.gtsn.lib.ui.demo;

import com.gtsn.lib.ui.sync.SyncedMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * 数据同步演示菜单（#19 的游戏内验证载体）：服务端每 {@link DemoSync#PROGRESS_PERIOD_TICKS} tick
 * 推进一次同步值（进度 / 阶段 / 档位 / 速度 / 运行开关），经 {@code SyncedMenu} 的数据槽自动同步到客户端。
 *
 * <p>无物品槽；打开方式：服务端命令 {@code /gtsnlib ui}（经 {@link NetworkHooks#openScreen}），
 * 客户端屏幕为 {@code DemoMenuScreen}。构造器在两端各执行一次（客户端经 {@code IContainerFactory}），
 * 仅服务端执行推进。</p>
 */
public final class DemoMenu extends SyncedMenu {

    private static final Component TITLE = Component.literal("GTSN UI Sync Demo");

    private final boolean serverSide;
    private int serverTicks;

    public DemoMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory) {
        super(menuType, containerId, DemoSync.LAYOUT);
        this.serverSide = !playerInventory.player.level().isClientSide;
    }

    /** 网络工厂（{@code IContainerFactory}）：客户端经 {@code MenuType.create} 走此路径创建菜单实例。 */
    public static DemoMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        return new DemoMenu(DemoMenus.SYNC_DEMO.get(), containerId, playerInventory);
    }

    /** 菜单提供者（服务端命令 / 交互触发打开）。 */
    public static MenuProvider provider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return TITLE;
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new DemoMenu(DemoMenus.SYNC_DEMO.get(), containerId, inventory);
            }
        };
    }

    /** 服务端是否已推进过（诊断 / 测试用）。 */
    public int serverTicks() {
        return serverTicks;
    }

    @Override
    public void broadcastChanges() {
        if (serverSide) {
            serverTicks++;
            if (serverTicks % DemoSync.PROGRESS_PERIOD_TICKS == 0) {
                advanceServerValues();
            }
        }
        super.broadcastChanges();
    }

    /** 服务端推进规则：进度回卷时轮换档位 / 速度 / 运行开关（纯规则见 {@link DemoSync}）。 */
    private void advanceServerValues() {
        int progress = DemoSync.advanceProgress(sync().get(DemoSync.PROGRESS), DemoSync.PROGRESS_STEP);
        sync().set(DemoSync.PROGRESS, progress);
        sync().set(DemoSync.PHASE, DemoSync.phaseFor(progress));
        if (progress <= DemoSync.PROGRESS_MIN) {
            int tier = DemoSync.nextTier(sync().get(DemoSync.TIER));
            sync().set(DemoSync.TIER, tier);
            sync().set(DemoSync.SPEED, DemoSync.speedForTier(tier));
            sync().set(DemoSync.ACTIVE, tier % 2 == 0);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
