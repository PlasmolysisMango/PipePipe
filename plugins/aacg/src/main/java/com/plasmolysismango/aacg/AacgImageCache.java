package com.plasmolysismango.aacg;

import org.schabi.newpipe.extractor.plugin.PluginImage;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small LRU for decrypted covers: 32 entries, 24 MiB and one hour. */
final class AacgImageCache {
    private static final int MAX_ENTRIES = 32;
    private static final long MAX_BYTES = 24L * 1024L * 1024L;
    private static final long TTL_MILLIS = 60L * 60L * 1000L;

    private static final class Entry {
        final PluginImage image;
        final int size;
        final long storedAt;

        Entry(final PluginImage image, final int size, final long storedAt) {
            this.image = image;
            this.size = size;
            this.storedAt = storedAt;
        }
    }

    private final LinkedHashMap<String, Entry> entries =
            new LinkedHashMap<String, Entry>(16, 0.75f, true);
    private long bytes;

    synchronized PluginImage get(final String key) {
        final Entry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() - entry.storedAt >= TTL_MILLIS) {
            entries.remove(key);
            bytes -= entry.size;
            return null;
        }
        return entry.image;
    }

    synchronized void put(final String key, final PluginImage image) {
        final Entry previous = entries.remove(key);
        if (previous != null) {
            bytes -= previous.size;
        }
        final int size = image.getData().length;
        entries.put(key, new Entry(image, size, System.currentTimeMillis()));
        bytes += size;
        final Iterator<Map.Entry<String, Entry>> iterator = entries.entrySet().iterator();
        while ((entries.size() > MAX_ENTRIES || bytes > MAX_BYTES) && iterator.hasNext()) {
            final Map.Entry<String, Entry> eldest = iterator.next();
            bytes -= eldest.getValue().size;
            iterator.remove();
        }
    }
}
