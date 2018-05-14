#!/usr/bin/env groovy
import hudson.model.Result
import hudson.model.Run

/**
 * Global variable to hold routines and utilities for more comfortable work with Slack
 */

String buildColor() {
    return getBuildColor()
}

String getBuildColor() {
    Run build = $build()
    Result result = build.result ?: Result.SUCCESS
    if (result == Result.SUCCESS) {
        return 'good'
    }

    if (result == Result.UNSTABLE) {
        return 'warning'
    }

    if (result == Result.FAILURE) {
        return 'danger'
    }
    return null
}
