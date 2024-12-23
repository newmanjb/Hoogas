package com.noomtech.hoogas.monitoring;


/**
 * Used by interested parties to obtain a feed of updates for the state of the Hoogas applications e.g up, down, connections, num msgs sent etc..
 * Often used by custom components that drive the updating of a UI that displays a visual representation of the state of the
 * applications that are managed by Hoogas.
 * Each update is a snapshot of the entire system as it was when the message was sent.
 * @author Joshua Newman, December 2024
 */
public interface HoogasApplicationsStateListener {

    /**
     * Any time-consuming operations in this listener should be implemented in a separate thread, otherwise they will hold up the main Hoogas thread.
     * @param snapshot
     */
    void onStateChanged(String snapshot);
}
