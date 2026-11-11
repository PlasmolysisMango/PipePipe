package com.plasmolysismango.ppipecore.test;

import org.schabi.newpipe.extractor.plugin.PluginPreferences;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** In-memory {@link PluginPreferences} with optional plugin.json defaults. */
public final class FakePreferences implements PluginPreferences {
    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, String> defaults;
    private final List<ChangeListener> listeners = new ArrayList<>();

    public FakePreferences() {
        this(new LinkedHashMap<>());
    }

    public FakePreferences(final Map<String, String> defaults) {
        this.defaults = new LinkedHashMap<>(defaults);
    }

    @Nullable
    @Override
    public String getString(@Nonnull final String key) {
        final String value = values.get(key);
        return value != null ? value : defaults.get(key);
    }

    @Override
    public boolean getBoolean(@Nonnull final String key) {
        final String value = getString(key);
        return value != null && Boolean.parseBoolean(value);
    }

    @Override
    public void putString(@Nonnull final String key, @Nullable final String value) {
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
        for (final ChangeListener listener : listeners) {
            listener.onChanged(key);
        }
    }

    @Override
    public void putBoolean(@Nonnull final String key, final boolean value) {
        putString(key, String.valueOf(value));
    }

    @Override
    public void addChangeListener(@Nonnull final ChangeListener listener) {
        listeners.add(listener);
    }
}
