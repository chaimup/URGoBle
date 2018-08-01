package com.upright.goble.events;

import lombok.Data;

@Data
public class URGReadEvent {
    int value;

    public URGReadEvent(int value) {
        this.value = value;
    }
}
