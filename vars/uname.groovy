#!/usr/bin/env groovy

def machine() {
    return call("-m")
}

def kernel() {
    return call("-s")
}

def os() {
    return call("-o")
}

def procesor() {
    return call("-p")
}

def call(flag="") {
  sh(script: "uname ${flag}".trim(), returnStdout: true).trim()
}
