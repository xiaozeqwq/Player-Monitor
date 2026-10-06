package com.xiaoze.playermonitor.util;

import java.nio.charset.StandardCharsets;

/** Small helpers for UTF-8 conversion and safe string handling. */
public final class TextUtil {
    private TextUtil() {
    }

    public static byte[] toBytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    public static String fromBytes(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }

    public static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
