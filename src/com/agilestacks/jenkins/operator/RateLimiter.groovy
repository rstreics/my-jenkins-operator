package com.agilestacks.jenkins.operator

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Rate limiting single threaded queue. Needed to throttle calls to Kubernetes,
 * So API server should not be overloaded otherwise it can go panic
 */
class RateLimiter implements Runnable {
    final static CAPACITY = 1024
    final queue = new ArrayBlockingQueue<Closure>(CAPACITY)
    final scheduler = Executors.newSingleThreadScheduledExecutor()

    final rate = 32

    synchronized def enqueue(Closure item) {
        queue.add( item )
    }

    def size() {
        queue.size()
    }

    void shutdown() {
        scheduler.shutdown()
    }

    void startAtFixedRate(long millis=1000) {
        scheduler.scheduleAtFixedRate(this, 0L,  millis, TimeUnit.MILLISECONDS)
    }

    void startOnce(long millis=1000) {
        while(!queue.empty) {
            scheduler.execute(this)
            sleep(millis)
        }
    }

    @Override
    void run() {
        List<Closure> items = []
        queue.drainTo(items, rate)
        items.each { body ->
            body()
        }
    }
}
