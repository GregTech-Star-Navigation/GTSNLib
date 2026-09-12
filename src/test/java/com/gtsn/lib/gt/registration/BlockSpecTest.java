package com.gtsn.lib.gt.registration;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BlockSpec} 声明构建与校验的行为验证（#15）：不依赖任何 Minecraft / Forge / GTCEu 类型。
 *
 * <p>覆盖外部可观察行为：必填 id/namespace 归一化、属性默认值、强度/亮度范围、是否生成方块物品、
 * 显示名、注册钩子存取。</p>
 */
class BlockSpecTest {

    @Test
    void buildsWithAllDeclarations() {
        RegistrationHook hook = (kind, id) -> {
        };

        BlockSpec spec = BlockSpec.builder("gtsnlib", "test_block")
                .strength(2.0F, 3.0F)
                .lightLevel(7)
                .withItem(false)
                .displayName("Test Block")
                .hook(hook)
                .build();

        assertEquals("gtsnlib", spec.namespace());
        assertEquals("test_block", spec.id());
        assertEquals("gtsnlib:test_block", spec.key());
        assertEquals(2.0F, spec.destroyTime());
        assertEquals(3.0F, spec.explosionResistance());
        assertEquals(7, spec.lightLevel());
        assertFalse(spec.withItem());
        assertEquals(Optional.of("Test Block"), spec.displayName());
        assertEquals(Optional.of(hook), spec.hook());
    }

    @Test
    void defaultsMatchStoneLikeBlock() {
        BlockSpec spec = BlockSpec.builder("gtsnlib", "test_block").build();

        assertEquals(BlockSpec.DEFAULT_DESTROY_TIME, spec.destroyTime());
        assertEquals(BlockSpec.DEFAULT_EXPLOSION_RESISTANCE, spec.explosionResistance());
        assertEquals(0, spec.lightLevel());
        assertTrue(spec.withItem());
        assertTrue(spec.displayName().isEmpty());
        assertTrue(spec.hook().isEmpty());
    }

    @Test
    void normalizesNamespaceAndId() {
        BlockSpec spec = BlockSpec.builder(" GTSNLib ", " Test-Block ").build();

        assertEquals("gtsnlib", spec.namespace());
        assertEquals("test_block", spec.id());
        assertEquals("gtsnlib:test_block", spec.key());
    }

    @Test
    void strengthSetsBothValues() {
        BlockSpec spec = BlockSpec.builder("gtsnlib", "test_block")
                .strength(4.5F, 5.5F)
                .build();

        assertEquals(4.5F, spec.destroyTime());
        assertEquals(5.5F, spec.explosionResistance());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> BlockSpec.builder("gtsnlib", "  ").build());
    }

    @Test
    void rejectsDullIdThatNormalizesToNothing() {
        assertThrows(IllegalArgumentException.class, () -> BlockSpec.builder("gtsnlib", "!@#").build());
    }

    @Test
    void rejectsBlankNamespace() {
        assertThrows(IllegalArgumentException.class, () -> BlockSpec.builder(" ", "test_block").build());
    }

    @Test
    void rejectsNegativeStrength() {
        assertThrows(IllegalArgumentException.class,
                () -> BlockSpec.builder("gtsnlib", "test_block").destroyTime(-1.0F));
        assertThrows(IllegalArgumentException.class,
                () -> BlockSpec.builder("gtsnlib", "test_block").explosionResistance(-0.5F));
    }

    @Test
    void rejectsOutOfRangeLightLevel() {
        assertThrows(IllegalArgumentException.class,
                () -> BlockSpec.builder("gtsnlib", "test_block").lightLevel(-1));
        assertThrows(IllegalArgumentException.class,
                () -> BlockSpec.builder("gtsnlib", "test_block").lightLevel(16));
    }

    @Test
    void rejectsBlankDisplayName() {
        assertThrows(IllegalArgumentException.class,
                () -> BlockSpec.builder("gtsnlib", "test_block").displayName("  "));
    }
}
