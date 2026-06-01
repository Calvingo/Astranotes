package com.astraNotes.plugin;

import com.astraNotes.model.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PluginManager {
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);

    private final List<ITrustedPlugin> plugins = new ArrayList<>();
    private PluginStateStore pluginStateStore;

    public void addPlugin(ITrustedPlugin plugin) {
        plugins.add(plugin);
    }

    public void setPluginStateStore(PluginStateStore store) {
        this.pluginStateStore = store;
    }

    public Optional<String> getPluginState(String pluginId, String stateKey) {
        if (pluginStateStore == null) {
            return Optional.empty();
        }
        try {
            return pluginStateStore.getState(pluginId, stateKey);
        } catch (Exception e) {
            logger.error("Failed to get plugin state", e);
            return Optional.empty();
        }
    }

    public boolean savePluginState(String pluginId, String stateKey, String stateValue) {
        if (pluginStateStore == null) {
            logger.warn("Plugin state store is not configured");
            return false;
        }
        try {
            pluginStateStore.saveState(pluginId, stateKey, stateValue);
            return true;
        } catch (Exception e) {
            logger.error("Failed to save plugin state", e);
            return false;
        }
    }

    public boolean notifyBeforeCreate(Note note) {
        for (ITrustedPlugin p : plugins) {
            try {
                if (!p.beforeCreate(note)) {
                    logger.info("Plugin vetoed create for note {}", note.getId());
                    return false;
                }
            } catch (Exception e) {
                logger.error("Plugin beforeCreate threw", e);
            }
        }
        return true;
    }

    public void notifyAfterUpdate(Note note) {
        for (ITrustedPlugin p : plugins) {
            try {
                p.afterUpdate(note);
            } catch (Exception e) {
                logger.error("Plugin afterUpdate threw", e);
            }
        }
    }

    public boolean notifyBeforeDelete(String id) {
        for (ITrustedPlugin p : plugins) {
            try {
                if (!p.beforeDelete(id)) {
                    logger.info("Plugin vetoed delete for note {}", id);
                    return false;
                }
            } catch (Exception e) {
                logger.error("Plugin beforeDelete threw", e);
            }
        }
        return true;
    }

    public String notifyOnSearch(String query) {
        String q = query;
        for (ITrustedPlugin p : plugins) {
            try {
                String modified = p.onSearch(q);
                if (modified != null) {
                    q = modified;
                }
            } catch (Exception e) {
                logger.error("Plugin onSearch threw", e);
            }
        }
        return q;
    }
}
