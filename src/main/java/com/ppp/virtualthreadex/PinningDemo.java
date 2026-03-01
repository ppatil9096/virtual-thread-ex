package com.ppp.virtualthreadex;


/**
 * Demonstrates potential pinning by using synchronized + blocking call.
 * Run, then inspect JFR for pinned virtual threads and carrier utilization.
 */
public class PinningDemo {
    private final Object monitor = new Object();

    public static void main(String[] args) throws InterruptedException {
        JfrStarter.startRecording("pinning-demo");

        PinningDemo demo = new PinningDemo();

        // Create multiple virtual threads that will compete for the synchronized block
        Thread[] threads = new Thread[10];

        for (int i = 0; i < threads.length; i++) {
            final int threadId = i;
            threads[i] = Thread.ofVirtual().start(() -> {
                demo.synchronizedBlockingOperation(threadId);
            });
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("All threads completed. Check JFR recording for pinning events.");
    }

    private void synchronizedBlockingOperation(int threadId) {
        synchronized (monitor) {
            System.out.println("Thread " + threadId + " entered synchronized block");
            try {
                // Simulate blocking I/O or long computation that can cause pinning
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Thread " + threadId + " exiting synchronized block");
        }
    }
}
