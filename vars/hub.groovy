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
            build.getEnvironment(new LogTaskListener(log, Level.INFO)).get(name)
}

def elaborate(Map args=[:]) {
    final argv = [
        manifest: ['hub.yaml', 'hub-application.yaml', 'hub-component.yaml', '.hub/hub.yaml'].find { fileExists( it ) } ?: 'hub.yaml',
        elaborate: 'hub.yaml.elaborate',
        state: getParamOrEnvvarValue('STATE_FILE') ?: 'hub.yaml.state',
    ] << args

    final result = sh( returnStatus: true, script: "hub elaborate ${argv.manifest} -s ${argv.state} -o ${argv.elaborate}" )
    if (result != 0) {
        error "Error [code: ${result}] during hub elaborate"
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
        error "hub deploy finished with error [code: ${result}]"
    }
}

def explain(String arg) {
    explain(elaborate: arg)
}

def explain(Map args=[:]) {
    final argv = [
        state: getParamOrEnvvarValue('STATE_FILE') ?: 'hub.yaml.state',
        tag: 'hub',
    ] << args

    final content = sh(script: "hub explain ${argv.state} --json | jq -cM .", returnStdout: true).trim()
    final result = sh(script: 'echo -n $?', returnStdout: true).trim()
    if (result != '0') {
        error "hub explain finished with error [code: ${result}]\n---\n${content}"
    }
    return new JsonSlurperClassic().parseText(content as String)
}

def render(String template, Map additional=[:]) {
    render([template: template, additional: additional])
}

def kubeconfig(String caCert, String clientCert, String clientKey,
    String apiEndpoint, String domain){
    kubeconfig([
        caCert: caCert,
        clientCert: clientCert,
        clientKey: clientKey,
        apiEndpoint: apiEndpoint,
        domain: domain
        ])
}

def kubeconfig(String arg) {
    kubeconfig(state: arg)
}

def kubeconfig(Map args=[:]) {
    final argv = [
        state: getParamOrEnvvarValue('PLATFORM_STATE_FILE') ?: 'hub.yaml.state'
    ] << args

    if (argv.caCert && argv.clientCert && argv.clientKey
        && argv.apiEndpoint && argv.domain) {
        sh(returnStatus: false,
            script: "#!/bin/sh -e\n echo \"${argv.caCert}\" > caCert.pom; echo \"${argv.clientCert}\" > clientCert.pom; "+
                "echo \"${argv.clientKey}\" > clientKey.pom")
        sh(returnStatus: false,
            script: "kubectl config set-cluster ${argv.domain} "+
                "--embed-certs=true "+
                "--server=https://${argv.apiEndpoint} "+
                "--certificate-authority=caCert.pom")
        sh(returnStatus: false,
            script: "kubectl config set-credentials admin@${argv.domain} "+
                "--embed-certs=true "+
                "--client-key=clientKey.pom "+
                "--client-certificate=clientCert.pom")
        sh(returnStatus: false,
            script: "kubectl config set-context ${argv.domain}-context "+
                "--cluster=${argv.domain} "+
                "--user=admin@${argv.domain} "+
                "--namespace=kube-system")
        final result = sh(returnStatus: true,
            script: "kubectl config use-context ${argv.domain}-context")
        if (result != 0) {
            error "kubectl config use-context ${argv.domain}-context exited with error [code: ${result}]"
        }
    } else {
        def command = "hub kubeconfig ${argv.state}"
        if (argv.switchContext) {
            command += " -k"
        }
        final result = sh returnStatus: true, script: command
        if (result != 0) {
            error "hub kubeconfig finished with error [code: ${result}]"
        }
    }
}

def render(Map args=[:]) {
    final argv = [
        state: getParamOrEnvvarValue('STATE_FILE') ?: 'hub.yaml.state',
    ] << args

    if (!argv.template || !fileExists(argv.template)) {
        error "Cannot find template file: ${argv.template}"
    }

    def command = "hub render ${argv.template} -s ${argv.state}"
    if (argv.component) {
        command += " -c ${argv.component}"
    }

    if (argv.additional) {
        command += " -a ${argv.additional.collect {k, v -> "${k}=${v}"}.join(',')}"
    }

    final result = sh returnStatus: true, script: command
    if (result != 0) {
        error "hub render finished with error [code: ${result}]"
    }
    return result
}
