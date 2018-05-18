#!/usr/bin/env groovy

properties([parameters([
    booleanParam(name: 'NO_SLACK', defaultValue: false, description: 'Do not send slack notification'),
    booleanParam(name: 'PRINT_STACKTRACE', defaultValue: false, description: 'Print exceptin stack trace'),
    booleanParam(name: 'CLEAN_WORKSPACE', defaultValue: false, description: 'Start with empty workspace'),
  ]),
  pipelineTriggers([
    [$class: 'GitHubPushTrigger'],
     pollSCM('H/15 * * * *')
  ])
])


  node('master') {
    if (params.CLEAN_WORKSPACE) {
      echo "Wiping out workspace"
      deleteDir()
    }

    stage('Checkout') {
      // checkout scm
      git credentialsId: 'asibot', url: 'https://github.com/agilestacks/jenkins.git'
    }
  }
try {
  def uuid = UUID.randomUUID().toString()

  podTemplate(
    label: uuid,
    containers: [
      containerTemplate(
        name: 'buildbox',
        image: 'openjdk:9-jdk-slim',
        ttyEnabled: true,
        command: 'cat'
      ),
    ],
    volumes: [
      // no needs to clean temp dirs
      emptyDirVolume(mountPath: "/home/jenkins/workspace/${files.pwdDirName(tmp: true)}"),
      emptyDirVolume(mountPath: '/dev/shm', memory: false),
    ]
  ) {
    node(uuid) {
      container('buildbox') {
        stage('Build') {
          sh script: './gradlew compileGroovy test'
        }
      }
    }
  }
} catch (err) {
  currentBuild.result = 'FAILURE'
  currentBuild.description = err.message ?: err.class.simpleName
  if (params.PRINT_STACKTRACE) {
    build.printStackTrace(err)
  }
} finally {
  container('allure') {
    junit testResults: 'build/**/TEST-*.xml',
          allowEmptyResults: false,
          keepLongStdio: true
    publishHTML [ reportName: 'Allure',
                  reportDir: env.SITE_DIRECTORY,
                  reportFiles: 'index.html',
                  reportTitles: '',
                  allowMissing: false,
                  alwaysLinkToLastBuild: false,
                  keepAll: false]
  }

  if (!params.NO_SLACK) {
    if (build.resultChanged) {
      slackSend color: slack.buildColor,
                message: slack.buildReport(htmlReport: 'Allure')
    }
  }
}
