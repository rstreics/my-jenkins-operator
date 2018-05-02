#!/usr/bin/env groovy

// @GrabResolver(name='releases', root='http://repo.jenkins-ci.org/releases/')
// @Grab(group='org.jenkins-ci.plugins.workflow', module='workflow-support', version='2.13')
import hudson.model.Result
import hudson.model.Run
//import org.jenkinsci.plugins.workflow.job.WorkflowRun

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

def becameFailure(Closure body=null) {
    Run build = $build()
    def result = build?.result ?: Result.SUCCESS
    if (result.isWorseOrEqualTo(Result.FAILURE)) {
        def prevResult = build?.previousBuild?.result ?: Result.SUCCESS
        if (prevResult.isBetterThan(Result.FAILURE)) {
            call(body)
        }
    }
}

def becameUnstable(Closure body=null) {
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
//    Run build = $build()
//    build.properties.each { k,v->
//        echo "${k} = ${v}"
//    }
//
//    echo build.allActions.toString()
//    build.allActions.each {
//        echo "Action: ${it.class.name}: ${it}"
//    }
    if (body != null) {
        def config = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()
    }
}
