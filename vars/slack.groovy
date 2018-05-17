#!/usr/bin/env groovy
import hudson.model.Result
import hudson.model.Run
import hudson.util.LogTaskListener

import java.util.logging.Level
import java.util.logging.Logger

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

String buildReport(Map args=[:]) {
    def argv = [
            includeTests: true,
            htmlReports: []
    ] << args

    if (argv.htmlReport) {
        argv.htmlReports << argv.htmlReport
    }

    Run currentBuild = $build()
    def env = currentBuild.getEnvironment(new LogTaskListener(build.LOGGER, Level.INFO))
    def result = "${build.getResultMessage()}: <${env.RUN_DISPLAY_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}>\n"

    if (currentBuild.description) {
        result += "Message: ${currentBuild.description}\n"
    }

    String testSummary = build.testSummary
    if (argv.includeTests && testSummary) {
        Map links = [ (build.blueOceanTestsPage) : testSummary ] <<
                        argv.htmlReports.collectEntries { label ->
                            def href = "${env.BUILD_URL}/${label.replaceAll(/\s/, "_20")}"
                            [ (href) : label ]
                        }

        def text = links.collect { href, label ->  "<${href}|${label}>" }.join(" | ")
        result += "Tests: ${text}\n"
    }
    if (build.recentCommitCount) {
        result += "Changes: <${build.blueOceanChangesPage}|${build.changesSummary}>\n"
    }
    return result
}

String getBuildReport() {
    buildReport()
}
