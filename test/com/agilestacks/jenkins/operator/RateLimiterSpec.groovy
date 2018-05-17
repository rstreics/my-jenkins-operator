package com.agilestacks.jenkins.operator

import spock.lang.Specification

class RateLimiterSpec extends Specification {
    RateLimiter queue

    def "queue is able to limit rates"() {
        given:
        def execNum = 0
        100.times {
            queue.enqueue {
                execNum++
                println "exec ${execNum}"
            }
        }

        when:
        queue.startOnce()

        then:
        100 == execNum
    }

    def setup() {
        queue = new RateLimiter()
    }

    def cleanup() {
        queue.shutdown()
    }
}
