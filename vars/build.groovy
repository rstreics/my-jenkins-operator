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
import jenkins.scm.RunWithSCM
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper
import org.jenkinsci.plugins.blueoceandisplayurl.BlueOceanDisplayURLImpl

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

boolean isResultChanged() {
    return isBackToNormal()  ||
            isBecomeFailure() ||
            isBecomeUnstable()
}

String resultMessage() {
    if (isBackToNormal()) {
        return "Back to normal"
    }
    Run build = $build()
    Result result = build.result ?: Result.SUCCESS
    return result.toString().toLowerCase().capitalize()
}

String getResultMessage() {
    if (isBackToNormal()) {
        return "Back to normal"
    }
    Run build = $build()
    Result result = build.result ?: Result.SUCCESS
    return result.toString().toLowerCase().capitalize()
}

def getBlueOceanBuildPage() {
    Run build = $build()
    new BlueOceanDisplayURLImpl().getRunURL( build )
}

def getBlueOceanChangesPage() {
    Run build = $build()
    new BlueOceanDisplayURLImpl().getChangesURL( build )
}

def getBlueOceanTestsPage() {
    "${getBlueOceanBuildPage()}/tests"
}

def getBlueOceanArtifactsPage() {
    "${getBlueOceanBuildPage()}/artifacts"
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

def getFailedTestsCount() {
    Run build = $build()
    return build.getAction(AbstractTestResultAction)?.failCount ?: 0
}

def printStackTrace(Throwable err) {
    def sw = new StringWriter()
    def pw = new PrintWriter(sw)
    err.printStackTrace(pw)
    echo "Exception ${err.class.name}: ${err.message}\nStack trace: ${sw.toString()}"
}

List<String> getBlameMessage() {
    return blameMessage()
}

List<String> blameMessage(args = [:]) {
    def argv = [
            author: false
    ] << args
    RunWithSCM build = $build()
    def changesets = []
    build.changeSets.each {
        if (!it.emptySet) {
            changesets << it.items
        }
    }
    return changesets.collect {
        argv.author \
                ? "#${it.commitId?.take(7)}: ${it.msg}"
                : "#${it.commitId?.take(7)}: ${it.msg} (${it.author})"
    }
}

def call() {
    new RunWrapper($build(), true)
}
