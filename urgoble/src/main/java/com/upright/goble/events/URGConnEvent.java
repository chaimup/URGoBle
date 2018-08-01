package com.upright.goble.events;

import com.polidea.rxandroidble2.RxBleConnection;

public class URGConnEvent {
    private State state;

    public enum State {
        Unknown,
        Connected,
        Disconnected
    }

    public URGConnEvent(RxBleConnection.RxBleConnectionState state) {
        switch (state) {
            case CONNECTED:
                this.state = State.Connected;
                break;
            case DISCONNECTED:
                this.state = State.Disconnected;
                break;
            default:
                this.state = State.Unknown;
        }
    }

    public State getState() {
        return state;
    }
}
