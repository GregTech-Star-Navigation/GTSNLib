package com.gtsn.lib.ui.render;

/**
 * 纹理引用（命名空间 + 路径），渲染抽象的 MC-free 纹理标识；
 * 由具体渲染实现翻译为平台资源位置。
 */
public record TextureRef(String namespace, String path) {

    public TextureRef {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("texture namespace must not be blank");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("texture path must not be blank");
        }
    }

    public static TextureRef of(String namespace, String path) {
        return new TextureRef(namespace, path);
    }

    /** 解析 {@code namespace:path} 形式，缺省命名空间为 {@code minecraft}。 */
    public static TextureRef parse(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("texture location must not be blank");
        }
        int separator = location.indexOf(':');
        if (separator < 0) {
            return new TextureRef("minecraft", location);
        }
        return new TextureRef(location.substring(0, separator), location.substring(separator + 1));
    }

    public String location() {
        return namespace + ":" + path;
    }

    @Override
    public String toString() {
        return location();
    }
}
