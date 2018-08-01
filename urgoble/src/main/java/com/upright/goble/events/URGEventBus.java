package com.upright.goble.events;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class URGEventBus {
    private static URGEventBus eventBus;

    public URGEventBus() {
    }

    private PublishSubject<Object> bus = PublishSubject.create();

    public void send(Object o) {
        bus.onNext(o);
    }

    public Observable<Object> toObservable() {
        return bus;
    }

    public static  URGEventBus bus() {
        return(eventBus == null ? new URGEventBus() : eventBus);
    }
}
