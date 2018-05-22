pipeline {
  triggers {
    githubPush()
    pollSCM('H/15 * * * *')
  }
  agent {
    kubernetes {
      label 'pod'
      containerTemplate {
        name 'buildbox'
        image 'openjdk:8-jdk-slim'
        ttyEnabled true
        command 'cat'
      }
    }
  }
  stages {
    stage('Checkout') {
      steps {
        checkout scm
        // git credentialsId: 'asibot', url: 'https://github.com/agilestacks/jenkins.git'
      }
    }
    stage('Build') {
      steps {
        container('buildbox') {
          sh script: './gradlew compileGroovy'
          sh script: './gradlew cleanTest test'
          sh script: './gradlew codenarcMain'
        }
      }
    }
  }
  post {
    always {
      junit testResults: 'build/test-results/**/*.xml'
      // container('buildbox') {
      //   sh script: './gradlew allureReport'
      // }
      publishHTML(target: [
                  allowMissing         : true,
                  alwaysLinkToLastBuild: false,
                  keepAll              : true,
                  reportDir            : 'build/reports/tests/test',
                  reportFiles          : 'index.html',
                  reportName           : 'Spock',])
      publishHTML(target: [
                  allowMissing         : true,
                  alwaysLinkToLastBuild: false,
                  keepAll              : true,
                  reportDir            : 'build/reports/codenarc',
                  reportFiles          : 'main.html',
                  reportName           : 'Lint',])
      // publishHTML(target: [
      //             allowMissing         : true,
      //             alwaysLinkToLastBuild: false,
      //             keepAll              : true,
      //             reportDir            : 'build/reports/allure-report',
      //             reportFiles          : 'index.html',
      //             reportName           : 'Allure',])
    }
    changed {
      slackSend color: slack.buildColor,
                message: slack.buildReport(htmlReports: ['Spock', 'Lint'])
    }
  }
}
