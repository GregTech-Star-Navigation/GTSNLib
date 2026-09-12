package com.gtsn.lib.testing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * 极简 TrueType/OpenType {@code cmap} 覆盖查询（测试专用，仅依赖 JDK）：
 * 解析 format 4（BMP）与 format 12（全平面）字符映射子表，判断某码位是否有字形。
 *
 * <p>用途（#23）：在无客户端环境中验证随库打包的字体子集到底覆盖了哪些字符，
 * 从而证明 {@code reference} 回退层是缺失字形的必要兜底。</p>
 */
public final class TtfCmap {

    private final byte[] data;

    private int format4Offset = -1;
    private int format12Offset = -1;

    private TtfCmap(byte[] data) {
        this.data = Objects.requireNonNull(data, "data");
        int numTables = u16(4);
        int cmapOffset = -1;
        for (int i = 0; i < numTables; i++) {
            int record = 12 + i * 16;
            if (tag(record).equals("cmap")) {
                cmapOffset = (int) u32(record + 8);
            }
        }
        if (cmapOffset < 0) {
            throw new IllegalArgumentException("font has no cmap table");
        }
        int subtableCount = u16(cmapOffset + 2);
        for (int i = 0; i < subtableCount; i++) {
            int record = cmapOffset + 4 + i * 8;
            int platform = u16(record);
            int encoding = u16(record + 2);
            int subtable = cmapOffset + (int) u32(record + 4);
            int format = u16(subtable);
            if (format == 12 && (platform == 3 || platform == 0)) {
                format12Offset = subtable;
            } else if (format == 4 && (platform == 3 && encoding == 1 || platform == 0)) {
                format4Offset = subtable;
            }
        }
        if (format4Offset < 0 && format12Offset < 0) {
            throw new IllegalArgumentException("font has no usable cmap subtable (format 4/12)");
        }
    }

    public static TtfCmap parse(byte[] data) {
        return new TtfCmap(data);
    }

    public static TtfCmap load(String classpathResource) throws IOException {
        try (InputStream stream = TtfCmap.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (stream == null) {
                throw new IOException("classpath resource missing: " + classpathResource);
            }
            return parse(stream.readAllBytes());
        }
    }

    /** 该码位是否有字形（优先 format 12，其次 format 4）。 */
    public boolean covers(int codePoint) {
        if (format12Offset >= 0 && coversFormat12(codePoint)) {
            return true;
        }
        return format4Offset >= 0 && coversFormat4(codePoint);
    }

    private boolean coversFormat12(int codePoint) {
        long groups = u32(format12Offset + 12);
        for (long i = 0; i < groups; i++) {
            int group = format12Offset + 16 + (int) i * 12;
            long start = u32(group);
            long end = u32(group + 4);
            if (codePoint >= start && codePoint <= end) {
                return true;
            }
        }
        return false;
    }

    private boolean coversFormat4(int codePoint) {
        if (codePoint > 0xFFFF) {
            return false;
        }
        int segCount = u16(format4Offset + 6) / 2;
        int endCodes = format4Offset + 14;
        int startCodes = endCodes + segCount * 2 + 2;
        int idDeltas = startCodes + segCount * 2;
        int idRangeOffsets = idDeltas + segCount * 2;
        for (int i = 0; i < segCount; i++) {
            int end = u16(endCodes + i * 2);
            if (codePoint > end) {
                continue;
            }
            int start = u16(startCodes + i * 2);
            if (codePoint < start) {
                return false; // 段按升序排列，第一个 end >= cp 的段即候选
            }
            int idRangeOffset = u16(idRangeOffsets + i * 2);
            int glyph;
            if (idRangeOffset == 0) {
                glyph = (codePoint + (short) u16(idDeltas + i * 2)) & 0xFFFF;
            } else {
                int address = idRangeOffsets + i * 2 + idRangeOffset + (codePoint - start) * 2;
                glyph = u16(address);
                if (glyph != 0) {
                    glyph = (glyph + (short) u16(idDeltas + i * 2)) & 0xFFFF;
                }
            }
            return glyph != 0;
        }
        return false;
    }

    private String tag(int offset) {
        return new String(data, offset, 4, java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    private int u16(int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private long u32(int offset) {
        return ((long) (data[offset] & 0xFF) << 24)
                | ((long) (data[offset + 1] & 0xFF) << 16)
                | ((long) (data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }
}
