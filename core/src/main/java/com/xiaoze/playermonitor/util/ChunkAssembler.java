package com.xiaoze.playermonitor.util;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Reassembles chunked payloads. Minecraft guarantees per-connection ordering, so
 * chunks simply arrive in index order and are appended sequentially.
 */
public final class ChunkAssembler {
    private final Map<String, ByteArrayOutputStream> buffers = new HashMap<>();
    private final Map<String, Integer> totals = new HashMap<>();

    /**
     * Accepts one chunk. Returns the fully assembled byte array when the last
     * chunk has been received, otherwise {@code null}.
     */
    public synchronized byte[] accept(String key, int index, int total, byte[] data) {
        ByteArrayOutputStream out = buffers.get(key);
        if (out == null) {
            out = new ByteArrayOutputStream();
            buffers.put(key, out);
            totals.put(key, total);
        }
        out.write(data, 0, data.length);
        if (index >= total - 1) {
            byte[] assembled = out.toByteArray();
            buffers.remove(key);
            totals.remove(key);
            return assembled;
        }
        return null;
    }

    /** Drops any partially received chunks for the given key. */
    public synchronized void discard(String key) {
        buffers.remove(key);
        totals.remove(key);
    }
}
