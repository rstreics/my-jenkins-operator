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

String getBlueOceanBuildPage() {
    Run build = $build()
    new BlueOceanDisplayURLImpl().getRunURL( build )
}

String getBlueOceanChangesPage() {
    Run build = $build()
    new BlueOceanDisplayURLImpl().getChangesURL( build )
}

String getBlueOceanTestsPage() {
    def blueOcean = getBlueOceanBuildPage()
    return blueOcean.endsWith("/") \
            ? "${blueOcean}tests"
            : "${blueOcean}/tests"
}

String getBlueOceanArtifactsPage() {
    def blueOcean = getBlueOceanBuildPage()
    return blueOcean.endsWith("/") \
            ? "${blueOcean}artifacts"
            : "${blueOcean}/artifacts"
}

def getTestSummary() {
    Run build = $build()
    def testResultAction = build.getAction(AbstractTestResultAction)
    testResultAction \
            ? "Passed: ${getTestPassCount()}, Failed: ${getTestFailCount()}, Skipped ${getTestSkipCount()}"
            : null
}

def getTestFailCount() {
    Run build = $build()
    build.getAction(AbstractTestResultAction)?.failCount ?: 0
}

def getTestSkipCount() {
    Run build = $build()
    build.getAction(AbstractTestResultAction)?.skipCount ?: 0
}

def getTestTotalCount() {
    Run build = $build()
    build.getAction(AbstractTestResultAction)?.totalCount ?: 0
}

def getTestPassCount() {
    getTestTotalCount() - getTestFailCount() - getTestSkipCount()
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

def getRecentCommitCount() {
    RunWithSCM build = $build()
    def changesets = []
    build.changeSets.each {
        if (!it.emptySet) {
            changesets += it.items
        }
    }
    return changesets.size()
}

def getChangesSummary() {
    return changesSummary()
}

def changesSummary(args=[:]) {
    RunWithSCM build = $build()
    def changesets = []
    build.changeSets.each {
        if (!it.emptySet) {
            changesets += it.items
        }
    }

    if (changesets.empty) {
        return "No recent changes"
    }

    def authors = changesets.collect {it.author}.unique()
    return "${changesets.size()} commit${changesets.size()>1? 's' : ''} by ${authors.join(', ')}"
}

def call() {
    new RunWrapper($build(), true)
}
