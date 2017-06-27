package com.kwizzad.log;

public interface ILoggerImplementation {

    void log(int logLevel, Throwable t);

    void log(int logLevel, Object s1, Object[] args);

    void log(int logLevel, Throwable throwable, Object s1, Object[] args);

}
