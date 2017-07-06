package com.kwizzad;

import io.reactivex.Scheduler;

public interface ISchedulers {
    Scheduler mainThread();

    Scheduler io();
}
