#!/usr/bin/env groovy

import groovy.json.JsonSlurperClassic

def elaborate(String args) {
    elaborate(elaborate: args)
}

def elaborate(Map args=[:]) {
//    Run build = $build()
//    final log = Logger.getLogger('hub')
//    final env = build.getEnvironment(new LogTaskListener(log, Level.INFO))
    def argv = [
        manifest: ['./hub.yaml', './hub-aplication.yaml'].find { fileExists( it ) },
        elaborate: 'hub.yaml.elaborate',
        state: 'hub.yaml.state',
    ] << args

    def result = sh( returnStatus: true,
                     script: "hub elaborate ${argv.manifest} -s ${argv.state} -o ${argv.elaborate}" )
    if (result != 0) {
        throw new RuntimeException("Error [code: ${result}] during hub elaborate")
    }
    return result
}

def deploy(String arg) {
    deploy(elaborate: args)
}

def deploy(Map args=[:]) {
    def argv = [
        elaborate: 'hub.yaml.elaborate',
        state: 'hub.yaml.state',
    ] << args

    def result = sh( returnStatus: true,
        script: "hub deploy --git-outputs=false ${argv.elaborate} -s ${argv.state}" )
    if (result != 0) {
        throw new RuntimeException("Error [code: ${result}] during hub deploy")
    }
}

def explain(String arg) {
    explain(elaborate: arg)
}

def explain(Map args=[:]) {
    def argv = [
        elaborate: 'hub.yaml.elaborate',
        state: 'hub.yaml.state',
        tag: 'hub',
    ] << args

    def content = sh(script: "hub explain ${argv.elaborate} ${argv.state} --json | jq -cM .", returnStdout: true).trim()
    def result = sh(script: 'echo -n $?', returnStdout: true).trim()
    if (result != '0') {
        echo "hub explain finished with: ${result}\n---\n${content}"
        throw new RuntimeException("Error [code: ${result}] during hub explain:\n${content}")
    }
    return new JsonSlurperClassic().parseText(content as String)
}
