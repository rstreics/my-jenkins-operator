#!/usr/bin/env groovy

def short() {
    sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
}

def long() {
    sh(script: 'git rev-parse --long HEAD', returnStdout: true).trim()
}

def call() {
  node('master') {
    short()
  }
}
