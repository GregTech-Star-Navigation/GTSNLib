package com.gtsn.lib.ui.demo;

import java.util.Objects;

/**
 * 开发测试界面的可观察状态：点击计数、开关、最近按键、已输入字符与进度值。
 */
public final class DemoState {

    private int clicks;
    private boolean toggled;
    private String lastKey = "(none)";
    private String typed = "";
    private double progress;

    public int clicks() {
        return clicks;
    }

    public void incrementClicks() {
        clicks++;
    }

    public boolean toggled() {
        return toggled;
    }

    public void toggle() {
        toggled = !toggled;
    }

    public String lastKey() {
        return lastKey;
    }

    public void lastKey(String lastKey) {
        this.lastKey = Objects.requireNonNull(lastKey, "lastKey");
    }

    public String typed() {
        return typed;
    }

    public void appendTyped(char codePoint) {
        if (typed.length() < 64) {
            typed += codePoint;
        }
    }

    public void clearTyped() {
        typed = "";
    }

    /** 归一化进度 [0, 1]。 */
    public double progress() {
        return progress;
    }

    public void progress(double value) {
        this.progress = Math.max(0, Math.min(1, value));
    }

    public void advanceProgress(double delta) {
        progress(progress + delta);
    }
}
