package com.gtsn.lib.gt.adapter;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.IMaterialRegistryManager;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@link GtBackend} 的生产实现：**本包内唯一**直接接触 GTCEu 材料 / tag prefix / 注册 API 的类。
 *
 * <p>已验证的 GTCEu 7.5.3 访问点（tag {@code v7.5.3-1.20.1}，与 Gradle 解析的
 * {@code gtceu-1.20.1-7.5.3-slim.jar} 一致）：</p>
 * <ul>
 *   <li>{@code GTCEuAPI.materialManager}（{@code IMaterialRegistryManager}，注释注明 Construction 阶段可用）</li>
 *   <li>{@code IMaterialRegistryManager#getRegisteredMaterials()}</li>
 *   <li>{@code Material#getName()/getResourceLocation()/getUnlocalizedName()/getChemicalFormula()}</li>
 *   <li>{@code TagPrefix.values()} / {@code TagPrefix#name} / {@code #getLowerCaseName()} / {@code #getUnlocalizedName()}</li>
 *   <li>{@code GTRegistrate.create(String)}</li>
 * </ul>
 *
 * <p>注意：7.5.3 的 {@code GTRegistries} **没有** {@code MATERIALS}/{@code TAG_PREFIXES} 字段
 * （已用 {@code javap} 对解析到的 slim jar 验证）；材料注册表经 {@code GTCEuAPI.materialManager} 访问。
 * 8.0 移除 {@code materialManager} 时，迁移点收敛在本类。</p>
 */
final class GtceBackend implements GtBackend {

    @Override
    public boolean available() {
        return GTCEuAPI.materialManager != null;
    }

    @Override
    public Collection<GtMaterialRef> materials() {
        IMaterialRegistryManager manager = GTCEuAPI.materialManager;
        if (manager == null) {
            return List.of();
        }
        List<GtMaterialRef> refs = new ArrayList<>();
        for (Material material : manager.getRegisteredMaterials()) {
            ResourceLocation id = material.getResourceLocation();
            refs.add(new GtMaterialRef(
                    material.getName(),
                    id.getNamespace(),
                    id.getPath(),
                    id.toString(),
                    material.getUnlocalizedName(),
                    material.getChemicalFormula()));
        }
        return refs;
    }

    @Override
    public Collection<GtTagPrefixRef> tagPrefixes() {
        List<GtTagPrefixRef> refs = new ArrayList<>();
        for (TagPrefix prefix : TagPrefix.values()) {
            refs.add(new GtTagPrefixRef(prefix.name, prefix.getLowerCaseName(), prefix.getUnlocalizedName()));
        }
        return refs;
    }

    @Override
    public GtRegistrateHandle registrate(String modId) {
        return GtRegistrateHandle.create(modId);
    }
}
