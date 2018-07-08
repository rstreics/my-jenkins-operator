#!/usr/bin/env groovy

import hudson.model.Result
import hudson.model.Run

def success(Closure body=null) {
    Run build = $build()
    def result = build?.result ?: Result.SUCCESS
    if (result.isBetterOrEqualTo(Result.SUCCESS)) {
        call(body)
    }
}

def failed(Closure body=null) {
    Run build = $build()
    def result = build?.result ?: Result.SUCCESS
    if (result.isWorseOrEqualTo(Result.FAILURE)) {
        call(body)
    }
}

def unstable(Closure body=null) {
    Run build = $build()
    def result = build?.result ?: Result.SUCCESS
    if (result.isWorseOrEqualTo(Result.UNSTABLE)) {
        call(body)
    }
}

def backToNormal(Closure body=null) {
    Run build = $build()
    def result = build?.result ?: Result.SUCCESS
    if (result.isBetterOrEqualTo(Result.SUCCESS)) {
        def prevResult = build?.previousBuild?.result ?: Result.SUCCESS
        if (prevResult.isWorseThan(Result.SUCCESS)) {
            call(body)
        }
    }
}

def becomeFailure(Closure body=null) {
    Run build = $build()
    def result = build?.result ?: Result.SUCCESS
    if (result.isWorseOrEqualTo(Result.FAILURE)) {
        def prevResult = build?.previousBuild?.result ?: Result.SUCCESS
        if (prevResult.isBetterThan(Result.FAILURE)) {
            call(body)
        }
    }
}

def becomeUnstable(Closure body=null) {
    Run build = $build()
    def result = build?.result ?: Result.SUCCESS
    if (result == Result.UNSTABLE) {
        def prevResult = build?.previousBuild?.result ?: Result.SUCCESS
        if (prevResult != Result.UNSTABLE ) {
            call(body)
        }
    }
}

def call(Closure body=null) {
    if (body != null) {
        def config = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()
    }
}
