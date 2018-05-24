package com.agilestacks.jenkins.operator

import com.agilestacks.jenkins.operator.crd.ScriptableResource
import com.agilestacks.jenkins.operator.crd.Status
import io.fabric8.kubernetes.client.CustomResource

class Pipeline extends CustomResource implements ScriptableResource, Status {
    final String definitionFile = '/pipeline/definition.yaml'
    final String createScriptFile = '/pipeline/create.groovy'
    final String deleteScriptFile = '/pipeline/delete.groovy'

    Map<String, ?> defaults = [
        branchSpec: '*/master',
        pipeline  : 'Jenkinsfile',
        origin    : 'agilestacks.io',
        startBuild: true
    ]

}
