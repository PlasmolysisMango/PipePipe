package com.plasmolysismango.ppipecore.test;

import org.schabi.newpipe.extractor.plugin.PluginContext;
import org.schabi.newpipe.extractor.plugin.PluginPreferences;

import javax.annotation.Nonnull;

/** Minimal host context for constructing plugin services in tests. */
public final class FakePluginContext implements PluginContext {
    private final String pluginId;
    private final PluginPreferences preferences;

    public FakePluginContext(final String pluginId, final PluginPreferences preferences) {
        this.pluginId = pluginId;
        this.preferences = preferences;
    }

    public FakePluginContext(final PluginPreferences preferences) {
        this("com.plasmolysismango.test", preferences);
    }

    @Nonnull
    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Nonnull
    @Override
    public PluginPreferences getPreferences() {
        return preferences;
    }
}
