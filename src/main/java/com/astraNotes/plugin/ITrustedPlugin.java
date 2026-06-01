package com.astraNotes.plugin;

import com.astraNotes.model.Note;

/**
 * Interface for trusted plugins that subscribe to lifecycle events.
 */
public interface ITrustedPlugin {
    /**
     * Called before a note is created. Return false to veto creation.
     */
    boolean beforeCreate(Note note);

    /**
     * Called after a note is updated.
     */
    void afterUpdate(Note note);

    /**
     * Called before a note is soft-deleted. Return false to veto.
     */
    boolean beforeDelete(String noteId);

    /**
     * Called when a search is performed. Plugin may return a modified query.
     */
    String onSearch(String query);
}
