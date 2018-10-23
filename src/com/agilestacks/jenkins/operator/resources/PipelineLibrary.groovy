package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.ScriptableResource
import groovy.util.logging.Log
import io.fabric8.kubernetes.client.CustomResource

@Log
class PipelineLibrary extends CustomResource implements ScriptableResource {
    final Definition definition = fromClassPath('/pipelinelibrary/definition.yaml') as Definition
    final String deleteScript   = fromClassPath('/pipelinelibrary/delete.groovy')

    final Map<String, ?> getDefaults() {
        final result = [
            version: 'master',
            implicit  : false,
            allowVersionOverride: true,
            includeInChangesets: true,
        ]

        if (spec.retrievalMethod?.containsKey('git')) {
            result.retrievalMethod = [
                git: [
                    remote: 'origin',
                    credentialsId: null,
                    refSpec: '+refs/heads/*:refs/remotes/origin/*',
                    includes: '*',
                    excludes: ''
                ]
            ]
        }
        return result
    }

    String getCreateScriptFilename() {
        if (spec.retrievalMethod?.containsKey('git')) {
            return '/pipelinelibrary/create-git.groovy'
        }
        if (spec.retrievalMethod?.containsKey('fileSystem')) {
            return '/pipelinelibrary/create-fs.groovy'
        }
        throw new IllegalArgumentException( "Unsupported credentials type: ${this}" )
    }

    @Override
    String getCreateScript() {
        fromClassPath( this.createScriptFilename )
    }
}
