package com.gtsn.lib.ui.demo;

/**
 * 开发测试界面的主题控制接缝：客户端实现接全局主题上下文（{@code ThemeContext}），
 * 单测 / GameTest 可注入假实现验证按钮接线，不引入 MC 依赖。
 */
public interface ThemeControl {

    /** 当前主题展示名（按钮标签用）。 */
    String activeThemeName();

    /** 切换到下一个可用主题（注册顺序）。 */
    void nextTheme();
}
