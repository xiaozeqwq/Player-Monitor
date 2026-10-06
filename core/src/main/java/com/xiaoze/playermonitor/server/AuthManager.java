package com.xiaoze.playermonitor.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Verifies the admin password (SHA-256) and tracks authorized administrators for this session. */
public final class AuthManager {
    private static final Set<UUID> AUTHORIZED = ConcurrentHashMap.newKeySet();

    private AuthManager() {
    }

    public static boolean isAuthorized(UUID uuid) {
        return AUTHORIZED.contains(uuid);
    }

    public static void authorize(UUID uuid) {
        AUTHORIZED.add(uuid);
    }

    public static void revoke(UUID uuid) {
        AUTHORIZED.remove(uuid);
    }

    public static void clear() {
        AUTHORIZED.clear();
    }

    /** Returns the lowercase hex SHA-256 digest of the given input. */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static boolean verify(String plainPassword, String expectedHash) {
        if (expectedHash == null || expectedHash.isEmpty()) {
            return false;
        }
        return sha256(plainPassword).equalsIgnoreCase(expectedHash);
    }
}
