package com.gtsn.lib.ui.sync;

import java.util.Arrays;

/**
 * 内存 {@link IntStore} 测试后端：模拟容器菜单数据槽（可越界检测为测试提供清晰失败信息）。
 */
public final class TestIntStore implements IntStore {

    private final int[] values;

    public TestIntStore(int size) {
        this.values = new int[size];
    }

    @Override
    public int get(int index) {
        return values[index];
    }

    @Override
    public void set(int index, int value) {
        values[index] = value;
    }

    @Override
    public int size() {
        return values.length;
    }

    /** 原始槽位快照（断言同步结果用）。 */
    public int[] snapshot() {
        return Arrays.copyOf(values, values.length);
    }
}
