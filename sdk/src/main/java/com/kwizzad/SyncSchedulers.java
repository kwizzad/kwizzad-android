package com.kwizzad;

import rx.Scheduler;
import rx.schedulers.Schedulers;

public class SyncSchedulers implements ISchedulers {
    @Override
    public Scheduler mainThread() {
        return Schedulers.immediate();
    }

    @Override
    public Scheduler io() {
        return Schedulers.immediate();
    }
}
