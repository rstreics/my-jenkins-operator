#!/usr/bin/env groovy

def shortHash() {
    sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
}

def longHash() {
    sh(script: 'git rev-parse --long HEAD', returnStdout: true).trim()
}

def call() {
  node('master') {
    return shortHash()
  }
}
