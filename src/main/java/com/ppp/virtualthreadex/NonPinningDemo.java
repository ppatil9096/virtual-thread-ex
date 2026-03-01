package com.ppp.virtualthreadex;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Non-pinning alternative using ReentrantLock and Structured Concurrency style decomposition.
 * Compare JFR with PinningDemo.
 */
public class NonPinningDemo {
    private final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
        JfrStarter.startRecording("non-pinning-demo");
        NonPinningDemo demo = new NonPinningDemo();
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10; i++) {
                executorService.submit(demo::nonPinnedWork);
            }
        }
    }

    private void nonPinnedWork() {
        // Keep critical sections short, do blocking outside the lock
        lock.lock();
        try {
            // tiny guarded state, no blocking I/O here
            cpuSpin(100_000);
        } finally {
            lock.unlock();
        }
        // Blocking I/O OUTSIDE the lock => VT can unmount
        sleep(80);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private static void cpuSpin(int iterations) {
        long x = 0;
        for (int i = 0; i < iterations; i++) x += i;
        if (x == -1) System.out.println();
    }
}
