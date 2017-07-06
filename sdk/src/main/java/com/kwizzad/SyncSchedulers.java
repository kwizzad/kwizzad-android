package com.kwizzad;


import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

public class SyncSchedulers implements ISchedulers {
    @Override
    public Scheduler mainThread() {
        return Schedulers.computation();
    }

    @Override
    public Scheduler io() {
        return Schedulers.computation();
    }
}
