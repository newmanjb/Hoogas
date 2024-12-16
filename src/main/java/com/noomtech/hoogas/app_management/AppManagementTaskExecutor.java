package com.noomtech.hoogas.app_management;


import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Used to execute one {@link Runnable} instance at a time and does not allow submission of other tasks if one is already running.
 * @author Joshua Newman, December 2024
 */
public class AppManagementTaskExecutor {


    private volatile boolean shutdown;
    private final Thread theThread;

    private final ReentrantLock taskRunningLock = new ReentrantLock();
    private final Condition condition = taskRunningLock.newCondition();
    private volatile Runnable task;


    AppManagementTaskExecutor(String appName) {

        theThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!shutdown) {
                    taskRunningLock.lock();
                    try {
                        //Put this in while loop rather than an if in case we get spurious wake-ups
                        while(task == null) {
                            condition.await();
                        }
                        task.run();
                    }
                    catch(InterruptedException  e){

                        if(!shutdown) {
                            throw new IllegalArgumentException(e);
                        }
                    }
                    finally {
                        task = null;
                        taskRunningLock.unlock();
                    }
                }
            }

        });

        theThread.setName(appName + "__manager");
        theThread.setDaemon(false);
        theThread.start();
    }

    /**
     * Submits a task for running.  If one
     * @param task
     * @return
     */
    boolean submitTask(Runnable task) {
        if(taskRunningLock.tryLock()) {
            try {
                this.task = task;
                condition.signal();
            }
            finally {
                taskRunningLock.unlock();
            }
            return true;
        }
        return false;
    }

    /**
     * Waits for the executor to finish its current task for the given timeout period.  If the executor is still alive afterwards then
     * it will interrupt the thread and wait for the given timeout period again.
     */
    void shutDown(long timeout) throws InterruptedException {
        shutdown = true;
        theThread.join(timeout);
        if(theThread.isAlive()) {
            System.out.println("Been waiting for " + timeout + "ms for thread '" + theThread.getName() + "' to terminate and " +
                    "it still hasn't.  Interrupting it and then waiting another " + timeout + "ms");
            theThread.interrupt();
            theThread.join(timeout);
            if(theThread.isAlive()) {
                System.out.println("Interrupting it and waiting again didn't work.  Something's wrong with the code");
            }
        }
     }

     boolean threadIsAlive() {
        return theThread.isAlive();
     }
}
