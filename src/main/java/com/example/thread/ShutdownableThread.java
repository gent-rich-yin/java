package com.example.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Adapted from Kafka source code
 * See https://github.com/apache/kafka/blob/trunk/server-common/src/main/java/org/apache/kafka/server/util/ShutdownableThread.java
 */
@Slf4j
public abstract class ShutdownableThread extends Thread {
    private final boolean isInterruptible;

    private final CountDownLatch shutdownInitiated = new CountDownLatch(1);
    private final CountDownLatch shutdownComplete = new CountDownLatch(1);

    private volatile boolean isStarted = false;

    public ShutdownableThread(String name) {
        this(name, true);
    }

    public ShutdownableThread(String name, boolean isInterruptible) {
        super(name);
        this.isInterruptible = isInterruptible;
        this.setDaemon(false);
    }

    public void shutdown() throws InterruptedException {
        initiateShutdown();
        awaitShutdown();
    }

    public boolean isShutdownInitiated() {
        return shutdownInitiated.getCount() == 0;
    }

    public boolean isShutdownComplete() {
        return shutdownComplete.getCount() == 0;
    }

    /**
     * @return true if there has been an unexpected error and the thread shut down
     */
    // mind that run() might set both when we're shutting down the broker
    // but the return value of this function at that point wouldn't matter
    public boolean isThreadFailed() {
        return isShutdownComplete() && !isShutdownInitiated();
    }

    /**
     * @return true if the thread hasn't initiated shutdown already
     */
    public boolean initiateShutdown() {
        synchronized (this) {
            if (isRunning()) {
                log.info("Shutting down");
                shutdownInitiated.countDown();
                if (isInterruptible)
                    interrupt();
                return true;
            } else
                return false;
        }
    }

    /**
     * After calling initiateShutdown(), use this API to wait until the shutdown is complete.
     */
    public void awaitShutdown() throws InterruptedException {
        if (!isShutdownInitiated())
            throw new IllegalStateException("initiateShutdown() was not called before awaitShutdown()");
        else {
            if (isStarted)
                shutdownComplete.await();
            log.info("Shutdown completed");
        }
    }

    /**
     * Causes the current thread to wait until the shutdown is initiated,
     * or the specified waiting time elapses.
     *
     * @param timeout wait time in units.
     * @param unit    TimeUnit value for the wait time.
     */
    public void pause(long timeout, TimeUnit unit) throws InterruptedException {
        if (shutdownInitiated.await(timeout, unit))
            log.trace("shutdownInitiated latch count reached zero. Shutdown called.");
    }

    /**
     * This method is repeatedly invoked until the thread shuts down or this method throws an exception
     */
    public abstract void doWork() throws Exception;

    public void run() {
        isStarted = true;
        log.info("Starting");
        try {
            while (isRunning())
                doWork();
        } catch (Exception e) {
            shutdownInitiated.countDown();
            shutdownComplete.countDown();
            log.info("Stopped");
            Runtime.getRuntime().exit(-1);
        } finally {
            shutdownComplete.countDown();
        }
        log.info("Stopped");
    }

    public boolean isRunning() {
        return !isShutdownInitiated();
    }

}
