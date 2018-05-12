#!/usr/bin/env groovy
/**
 * This global variable provides easy access to current build routines.
 * @link isSuccess()
 */
//@GrabResolver(name='releases', root='http://repo.jenkins-ci.org/releases/')
//@Grab(group='org.jenkins-ci.plugins.workflow', module='workflow-support', version='2.13')
//@Grab(group='org.jenkins-ci.plugins.workflow', module='workflow-support', version='2.14')
//@Grab(group='org.jenkins-ci.plugins', module='junit', version='1.21')
import hudson.tasks.test.AbstractTestResultAction
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

def appendDescription(String text) {
    setAppendDescription(text)
    return getDescription()
}

void setAppendDescription(String text) {
    if (!text) {
        return
    }

    String oneline = getDescription().replaceAll('\n',';')
    List lines = oneline.split(';').collect{it?.trim()}.grep{it}
    lines.add(text)

    setDescription( lines.join('; ') )
}

void setDescription(String text) {
    Run build = $build()
    build.setDescription(text)
}

String getDescription() {
    Run build = $build()
    return build.description ?: ''
}

def leftShift(Exception err) {
    leftShift(err.message ?: err.toString())
}

def leftShift(String text) {
    Run build = $build()
    def result = Result.fromString(text)
    if (result) {
        build.result = result
    }
    setAppendDescription( text )
}

def testSummary(args = [:]) {
    def argv = [pretty: false] << args

    Run build = $build()
    def testResultAction = build.getAction(AbstractTestResultAction)

    if (testResultAction != null) {
        def total = testResultAction?.getTotalCount() ?: 0
        def failed = testResultAction?.getFailCount() ?: 0
        def skipped = testResultAction?.getSkipCount() ?: 0
        def passed = total - failed - skipped

        return "Passed: ${passed}, Failed: ${failed}, Skipped ${skipped}"
    }
    return argv.pretty ? "No tests available" : null
}

def printStackTrace(Throwable err) {
    def sw = new StringWriter()
    def pw = new PrintWriter(sw)
    err.printStackTrace(pw)
    echo "Exception ${err.toString()}\nStack trace: ${sw.toString()}"
}

def leftHandMenuDocument(Map args = [:]) {
    def argv = [text: null, scope: 'job'] << args
    Run build = $build()
    def path = URLEncoder.encode(argv.text, "UTF-8").replace("+", "_20")
    if (argv.scope == 'job') {
        return build.url.replaceAll("/${build.number}[/]?\$", "/${path}")
    }
    return "${build.url}/${path}"
}

def leftHandMenuDocument(String text) {
    return leftHandMenuLink([text: text])
}

def call() {
    new RunWrapper($build(), true)
}
