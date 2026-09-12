package com.gtsn.lib.ui.input;

/**
 * 输入常量：GLFW 键码与修饰键位。数值与 GLFW 一致，但此处不依赖 GLFW/Minecraft 类型。
 */
public final class Keys {

    public static final int SPACE = 32;
    public static final int ESCAPE = 256;
    public static final int ENTER = 257;
    public static final int TAB = 258;
    public static final int BACKSPACE = 259;
    public static final int DELETE = 261;
    public static final int RIGHT = 262;
    public static final int LEFT = 263;
    public static final int DOWN = 264;
    public static final int UP = 265;

    public static final int MOD_SHIFT = 0x1;
    public static final int MOD_CONTROL = 0x2;
    public static final int MOD_ALT = 0x4;

    private Keys() {
    }
}
