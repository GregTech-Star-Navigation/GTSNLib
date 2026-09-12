package com.gtsn.lib.ui.sync;

import java.util.Objects;

/**
 * 单个同步值槽位描述符：布局内的索引、名称、编解码器与默认值。
 *
 * <p>槽位由 {@link SyncLayout.Builder} 创建并归 {@link SyncLayout} 所有，本身不持有后端；
 * 读写经 {@link MenuSync} 进行（同一槽位描述符可同时用于服务端写与客户端读，避免两侧各自定义）。</p>
 *
 * @param <T> 类型化值
 */
public final class SyncSlot<T> {

    private final int index;
    private final String name;
    private final SyncCodec<T> codec;
    private final T defaultValue;

    SyncSlot(int index, String name, SyncCodec<T> codec, T defaultValue) {
        this.index = index;
        this.name = Objects.requireNonNull(name, "name");
        this.codec = Objects.requireNonNull(codec, "codec");
        // 默认值按编解码规则归一：编码可发现非法值（如非有限浮点）并钳制越界值，
        // 保证 defaultValue() 与实际写入后端后再读出的值一致。
        this.defaultValue = codec.decode(codec.encode(Objects.requireNonNull(defaultValue, "defaultValue")));
    }

    /** 槽位在后端中的索引（由布局按声明顺序分配）。 */
    public int index() {
        return index;
    }

    /** 槽位名称（布局内唯一，供诊断与按名查找）。 */
    public String name() {
        return name;
    }

    /** 编解码器。 */
    public SyncCodec<T> codec() {
        return codec;
    }

    /** 默认（初始）值。 */
    public T defaultValue() {
        return defaultValue;
    }

    /** 默认值的原始 int 表示。 */
    public int defaultRaw() {
        return codec.encode(defaultValue);
    }

    @Override
    public String toString() {
        return "SyncSlot[" + name + "#" + index + "]";
    }
}
