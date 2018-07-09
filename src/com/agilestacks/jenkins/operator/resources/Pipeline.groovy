package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.ScriptableResource
import groovy.json.JsonOutput
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
        startBuild: true,
        delay: 5
    ]

    @Override
    Map<String, ?> getMergedWithDefaults() {
        final p = JsonOutput.toJson( spec.parameters ?: [] )
        [kind: kind,
         apiVersion: apiVersion,
         spec: defaults + spec,
         metadata: metadata.properties,
         paramsBase64: p.bytes.encodeBase64(true)]
    }

    def addParameter(name, defaultValue, type='string', description='') {
        spec.parameters = spec.parameters ?: []
        spec.parameters << [ name: name,
                             defaultValue: defaultValue,
                             type: type,
                             description: description ]
    }

}
