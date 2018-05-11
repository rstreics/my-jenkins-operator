#!/usr/bin/env groovy

def call(def log) {
    def paramFormat = /\d{4}(?:\/\d{2}){2} \d{2}(?::\d{2}){2}\s+(?:\[.*\]){0,1}\s([\d\w-_\.]+:[\d\w-_\.]+) => `(.*)`/

    return log.split('Stack outputs:', 2)[1]
            .tokenize('\n')
            .collect { it =~ paramFormat }
            .findAll { it.matches() }
            .collectEntries { [(it.group(1)):it.group(2)] }
}
