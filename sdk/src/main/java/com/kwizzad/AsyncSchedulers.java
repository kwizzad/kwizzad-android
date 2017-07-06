package com.kwizzad;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class AsyncSchedulers implements ISchedulers {
    @Override
    public Scheduler mainThread() {
        return AndroidSchedulers.mainThread();
    }

    @Override
    public Scheduler io() {
        return Schedulers.io();
    }
}
