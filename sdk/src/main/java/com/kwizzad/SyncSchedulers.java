package com.kwizzad;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

public class SyncSchedulers implements ISchedulers {
    @Override
    public Scheduler mainThread() {
        return Schedulers.trampoline();
    }

    @Override
    public Scheduler io() {
        return Schedulers.trampoline();
    }
}
