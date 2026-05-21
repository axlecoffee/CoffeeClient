package com.replaymod.coffeeclient.hook.event;

public class CLReplayModInitEvent {
    private final Object replayModBackend;

    public CLReplayModInitEvent(Object replayModBackend) {
        this.replayModBackend = replayModBackend;
    }

    public Object getReplayModBackend() { return replayModBackend; }
}
