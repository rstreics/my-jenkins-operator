#!/usr/bin/env groovy

@GrabResolver(name='releases', root='http://repo.jenkins-ci.org/releases/')
@Grab(group='org.jenkins-ci.plugins.workflow', module='workflow-support', version='2.14')
@Grab(group='org.jenkins-ci.plugins', module='junit', version='1.21')
import hudson.tasks.test.AbstractTestResultAction
import hudson.model.Run
//import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

def call() {
    Run build = $build()
    def testResultAction = build.getAction(AbstractTestResultAction)

    if (testResultAction != null) {
        def total = testResultAction?.getTotalCount() ?: 0
        def failed = testResultAction?.getFailCount() ?: 0
        def skipped = testResultAction?.getSkipCount() ?: 0
        def passed = total - failed - skipped

       return "Passed: ${passed}, Failed: ${failed}, Skipped ${skipped}"
    }
    return "No tests found"
}
