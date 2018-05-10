#!/usr/bin/env groovy
/**
 * This global variable provides easy access to current build routines.
 * @link isSuccess()
 */
//@GrabResolver(name='releases', root='http://repo.jenkins-ci.org/releases/')
//@Grab(group='org.jenkins-ci.plugins.workflow', module='workflow-support', version='2.13')
import hudson.model.Result
import hudson.model.Run
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

/**
 * @return true if current build is success
 */
boolean isSuccess() {
    Run build = $build()
    def result = build?.result ?: Result.SUCCESS
    return result.isBetterOrEqualTo(Result.SUCCESS)
}

boolean isFailure() {
    Run build = $build()
    def result = build?.result ?: Result.SUCCESS
    return result.isWorseOrEqualTo(Result.FAILURE)
}

boolean isUnstable() {
    Run build = $build()
    def result = build?.result ?: Result.SUCCESS
    return result == Result.UNSTABLE
}

boolean isBackToNormal() {
    Run build = $build()
    if (isSuccess()) {
        def prevResult = build?.previousBuild?.result ?: Result.SUCCESS
        return prevResult.isWorseThan(Result.SUCCESS)
    }
    return false
}

boolean isBecomeFailure() {
    Run build = $build()
    if (isFailure()) {
        def prevResult = build?.previousBuild?.result ?: Result.SUCCESS
        return prevResult.isBetterThan(Result.FAILURE)
    }
    return false
}

boolean isBecomeUnstable() {
    Run build = $build()
    if (isUnstable()) {
        def prevResult = build?.previousBuild?.result ?: Result.SUCCESS
        return prevResult != Result.UNSTABLE
    }
    return false
}

void setAppendDescription(String text) {
    if (!text) {
        return
    }

    Run build = $build()
    def lines = this.description.
            replaceAll('\n', ';').
            split(';').
            collect{it.trim()}.
            grep{it}
    lines << text
    build.description = lines.join('; ')
}

void setDescription(String text) {
    Run build = $build()
    build.description = text
}

String getDescription() {
    Run build = $build()
    return build.description ?: ''
}

def leftShift(String text) {
    def result = Result.fromString(text)
    if (result) {
        Run build = $build()
        build.result = result
    }
    setAppendDescription( text )
}

def call() {
    new RunWrapper($build())
}
