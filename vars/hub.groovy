#!/usr/bin/env groovy

import groovy.json.JsonSlurperClassic
import hudson.model.ParametersAction
import hudson.model.Run
import hudson.util.LogTaskListener

import java.util.logging.Level
import java.util.logging.Logger

def elaborate(String args) {
    elaborate(elaborate: args)
}

def getParamOrEnvvarValue(String name) {
    Run build = $build()
    final params = build.getAction( ParametersAction )?.parameters ?: []
    final log = Logger.getLogger(this.class.name)
    return params.find { it.name == name }?.value ?:
            build.getEnvironment(new LogTaskListener(log, Level.INFO)).get()
}

def elaborate(Map args=[:]) {
    final argv = [
        manifest: ['./hub.yaml', './hub-application.yaml', './hub-component.yaml'].find { fileExists( it ) },
        elaborate: 'hub.yaml.elaborate',
        state: getParamOrEnvvarValue('STATE_FILE') ?: 'hub.yaml.state',
    ] << args

    final result = sh( returnStatus: true, script: "hub elaborate ${argv.manifest} -s ${argv.state} -o ${argv.elaborate}" )
    if (result != 0) {
        throw new RuntimeException("Error [code: ${result}] during hub elaborate")
    }
    return result
}

def deploy(String arg) {
    deploy(elaborate: args)
}

def deploy(Map args=[:]) {
    final argv = [
        elaborate: 'hub.yaml.elaborate',
        state: getParamOrEnvvarValue('STATE_FILE') ?: 'hub.yaml.state',
    ] << args

    final result = sh( returnStatus: true, script: "hub deploy --git-outputs=false ${argv.elaborate} -s ${argv.state}" )
    if (result != 0) {
        throw new RuntimeException("Error [code: ${result}] during hub deploy")
    }
}

def explain(String arg) {
    explain(elaborate: arg)
}

def explain(Map args=[:]) {
    final argv = [
        elaborate: 'hub.yaml.elaborate',
        state: getParamOrEnvvarValue('STATE_FILE') ?: 'hub.yaml.state',
        tag: 'hub',
    ] << args

    final content = sh(script: "hub explain ${argv.elaborate} ${argv.state} --json | jq -cM .", returnStdout: true).trim()
    final result = sh(script: 'echo -n $?', returnStdout: true).trim()
    if (result != '0') {
        echo "hub explain finished with: ${result}\n---\n${content}"
        throw new RuntimeException("Error [code: ${result}] during hub explain:\n${content}")
    }
    return new JsonSlurperClassic().parseText(content as String)
}
