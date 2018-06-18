package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.ScriptableResource
import groovy.util.logging.Log
import io.fabric8.kubernetes.client.CustomResource

@Log
class Pipeline extends CustomResource implements ScriptableResource {
    final String definitionFile = '/pipeline/definition.yaml'
    final String createScriptFile = '/pipeline/create.groovy'
    final String deleteScriptFile = '/pipeline/delete.groovy'

    final Map<String, ?> defaults = [
        branchSpec: '*/master',
        pipeline  : 'Jenkinsfile',
        origin    : 'agilestacks.io',
        startBuild: true
    ]

}
