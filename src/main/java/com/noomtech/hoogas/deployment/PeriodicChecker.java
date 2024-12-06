package com.noomtech.hoogas.deployment;


/**
 * Implemented by functionality in Hoogas that needs to be run at set intervals but which isn't very resource-heavy.
 * Allows a single thread to be used to check whether all these routines should be run and to run them.
 * @author Joshua Newman, December 2024
 */
public interface PeriodicChecker {


    default boolean checkShouldRun(long whenLastRunFinished) {
        return System.currentTimeMillis() - whenLastRunFinished >= getInterval();
    }

    void doCheck() throws Exception;

    long getInterval();
}
