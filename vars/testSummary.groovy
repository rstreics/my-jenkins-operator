#!/usr/bin/env groovy

import hudson.tasks.test.AbstractTestResultAction
import hudson.model.Run
//import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

def withUrl() {
    Run build = $build()
    def testResultAction = build.getAction(AbstractTestResultAction)
    if (testResultAction) {
        return """ \
                    ${call()}
                    Details: ${testResultAction.urlName}
               """.stripIndent().trim()
    }
    return "No tests found"
}

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
