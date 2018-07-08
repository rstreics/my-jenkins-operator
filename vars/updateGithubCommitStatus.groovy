#!/usr/bin/env groovy

import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper

private def getRepoURL() {
    sh(script: 'git config --get remote.origin.url', returnStdout: true).trim()
}

private def getCommitSha() {
    sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
}

def call(currentBuild = new RunWrapper($build(), true) ) {
    def repoUrl   = getRepoURL()
    def commitSha = getCommitSha()

    step([
            $class: 'GitHubCommitStatusSetter',
            reposSource: [$class: "ManuallyEnteredRepositorySource", url: repoUrl],
            commitShaSource: [$class: "ManuallyEnteredShaSource", sha: commitSha],
            errorHandlers: [[$class: 'ShallowAnyErrorHandler']],
            statusResultSource: [
                    $class: 'ConditionalStatusResultSource',
                    results: [
                            [$class: 'BetterThanOrEqualBuildResult', result: 'SUCCESS', state: 'SUCCESS', message: currentBuild.message],
                            [$class: 'BetterThanOrEqualBuildResult', result: 'FAILURE', state: 'FAILURE', message: currentBuild.message],
                            [$class: 'AnyBuildResult', state: 'FAILURE', message: currentBuild.message]
                    ]
            ]
    ])
}
