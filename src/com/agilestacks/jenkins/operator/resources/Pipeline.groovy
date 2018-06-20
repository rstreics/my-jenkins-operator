package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.ScriptableResource
import groovy.util.logging.Log
import io.fabric8.kubernetes.client.CustomResource

@Log
class Pipeline extends CustomResource implements ScriptableResource {
    final Definition definition = fromClassPath('/pipeline/definition.yaml') as Definition
    final String createScript   = fromClassPath('/pipeline/create.groovy')
    final String deleteScript   = fromClassPath('/pipeline/delete.groovy')

    final Map<String, ?> defaults = [
        branchSpec: '*/master',
        pipeline  : 'Jenkinsfile',
        origin    : 'agilestacks.io',
        startBuild: true
    ]

}
