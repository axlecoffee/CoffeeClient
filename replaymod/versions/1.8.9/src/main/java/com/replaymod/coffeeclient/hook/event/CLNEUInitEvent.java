package com.replaymod.coffeeclient.hook.event;

/** @deprecated Use {@link CLReplayModInitEvent} instead. */
@Deprecated
public class CLNEUInitEvent extends CLReplayModInitEvent {
    // NotEnoughUpdates will forever be in my heart
    public CLNEUInitEvent(Object replayModBackend) {
        super(replayModBackend);
    }
}
