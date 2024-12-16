package com.noomtech.hoogas.app_management;

import org.junit.jupiter.api.Test;

public class AppManagementTaskExecutorTest {


    //Run some tasks that take next to no time one after the other.  Ensure that there is no contention on submitting the tasks
    //i.e. a task is never submitted whilst another is still running
    @Test
    public void test1() throws Exception {
        final var booleans = new boolean[]{false, false, false};
        var appManagementTaskExecutor = new AppManagementTaskExecutor("TestApp1");
        try {
            assert(appManagementTaskExecutor.submitTask(new TestTask(0, 0, booleans)));
            Thread.sleep(10);
            assert(appManagementTaskExecutor.submitTask(new TestTask(1, 0, booleans)));
            Thread.sleep(10);
            assert(appManagementTaskExecutor.submitTask(new TestTask(2, 0, booleans)));
            Thread.sleep(10);

            assert(booleans[0] && booleans[1] && booleans[2]);
        }
        finally {
            appManagementTaskExecutor.shutDown(1000);
            assert(!appManagementTaskExecutor.threadIsAlive());
        }
    }

    //Run a task that takes a long time and perform a few submissions of another task while it's still running, then
    //wait till the task is finished and ensure that the second task can now be submitted.
    @Test
    public void test2() throws Exception {
        final var booleans = new boolean[]{false, false};
        var appManagementTaskExecutor = new AppManagementTaskExecutor("TestApp1");
        try {
            assert(appManagementTaskExecutor.submitTask(new TestTask(0, 1000, booleans)));
            Thread.sleep(200);
            assert(!appManagementTaskExecutor.submitTask(new TestTask(1, 0, booleans)));
            assert(!booleans[0] && !booleans[1]);
            Thread.sleep(200);
            assert(!appManagementTaskExecutor.submitTask(new TestTask(1, 0, booleans)));
            assert(!booleans[0] && !booleans[1]);
            Thread.sleep(900);
            assert(booleans[0] && !booleans[1]);
            assert(appManagementTaskExecutor.submitTask(new TestTask(1, 0, booleans)));
            Thread.sleep(200);
            assert(booleans[0] && booleans[1]);
        }
        finally {
            appManagementTaskExecutor.shutDown(1000);
            assert(!appManagementTaskExecutor.threadIsAlive());
        }
    }

    //Run a task that takes a long time and make sure that the executor still shuts down properly when told to even though the
    //shutdown timeout provided is less than the remaining duration that the task still has to run
    @Test
    public void test3() throws Exception {
        final var booleans = new boolean[]{false, false};
        var appManagementTaskExecutor = new AppManagementTaskExecutor("TestApp1");
        try {
            assert(appManagementTaskExecutor.submitTask(new TestTask(0, 8000, booleans)));
            appManagementTaskExecutor.shutDown(2000);
            assert(!appManagementTaskExecutor.threadIsAlive());
        }
        finally {
            appManagementTaskExecutor.shutDown(10000);
        }
    }


    class TestTask implements Runnable {
        private final int indexToUpdate;
        private final long timeToWait;
        private final boolean[] booleans;

        public TestTask(int indexToUpdate, long timeToWait, boolean[] booleans) {
            this.indexToUpdate = indexToUpdate;
            this.timeToWait = timeToWait;
            this.booleans = booleans;
        }
        @Override
        public void run() {
            if(timeToWait > 0) {
                try {
                    Thread.sleep(timeToWait);
                }
                catch(InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
            booleans[indexToUpdate] = true;
        }
    }
}
