package com.gtsn.lib.ui.theme;

/**
 * 主题解析失败（JSON 语法、字段类型、颜色格式或标识非法）。
 *
 * <p>加载层按文件捕获本异常：单个主题文件失败不影响其它主题，注册表保持上一次可用状态。</p>
 */
public class ThemeParseException extends RuntimeException {

    public ThemeParseException(String message) {
        super(message);
    }

    public ThemeParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
