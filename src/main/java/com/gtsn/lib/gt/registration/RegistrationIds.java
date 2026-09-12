package com.gtsn.lib.gt.registration;

import java.util.Locale;
import java.util.Objects;

/**
 * 注册简化层的内部命名工具（#15）：把声明式的 namespace / id 归一化为 {@code [a-z0-9_]}，
 * 并从 id 推导默认显示名。
 *
 * <p>包内工具，无 Minecraft / Forge / GTCEu 依赖；归一化行为与 {@link MaterialSpec} / {@link FluidSpec}
 * 保持一致，但集中一处供通用注册的规格复用。</p>
 */
final class RegistrationIds {

    private RegistrationIds() {
    }

    /**
     * 归一化为 {@code [a-z0-9_]}：去空白、转小写、非字母数字折叠为单个下划线并去除首尾下划线。
     *
     * @throws NullPointerException     输入为 {@code null}
     * @throws IllegalArgumentException 归一化后为空
     */
    static String normalize(String field, String raw) {
        Objects.requireNonNull(raw, field);
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        boolean lastWasUnderscore = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            boolean alphanumeric = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (alphanumeric) {
                out.append(c);
                lastWasUnderscore = false;
            } else if (!lastWasUnderscore && out.length() > 0) {
                out.append('_');
                lastWasUnderscore = true;
            }
        }
        int end = out.length();
        while (end > 0 && out.charAt(end - 1) == '_') {
            end--;
        }
        String normalized = out.substring(0, end);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must normalize to a non-empty id: " + raw);
        }
        return normalized;
    }

    /** 由归一化 id 推导 Title Case 显示名，如 {@code test_machine} → {@code Test Machine}。 */
    static String defaultDisplayName(String id) {
        StringBuilder out = new StringBuilder(id.length());
        for (String part : id.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
