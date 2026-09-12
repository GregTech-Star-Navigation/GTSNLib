package com.gtsn.lib.ui.demo;

/**
 * 数据同步演示的服务端阶段（经枚举编解码器同步；用于演示任意类型化值经数据槽往返）。
 */
public enum DemoPhase {
    IDLE,
    RUNNING,
    DONE
}
