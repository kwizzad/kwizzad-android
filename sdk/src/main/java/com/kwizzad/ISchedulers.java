package com.kwizzad;

import rx.Scheduler;

public interface ISchedulers {
    Scheduler mainThread();

    Scheduler io();
}
