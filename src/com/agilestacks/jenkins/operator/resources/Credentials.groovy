package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.jenkins.JenkinsHttpClient
import com.agilestacks.jenkins.operator.util.ScriptableResource
import groovy.util.logging.Log
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.client.KubernetesClient

@Log
class Credentials extends CustomResource implements ScriptableResource {
    final Definition definition = fromClassPath('/credentials/definition.yaml') as Definition
    final String deleteScript = fromClassPath('/credentials/delete.groovy')

    @Override
    def create(JenkinsHttpClient jenkins, KubernetesClient kubernetes) {
        Map secretRef
        if (spec.containsKey('usernamePassword')) {
            secretRef = spec.usernamePassword.password.secretKeyRef
        } else if (spec.containsKey('secretString')) {
            secretRef = spec.secretString.secretKeyRef
        } else {
            throw new IllegalArgumentException( "Unsupported credentials type: ${this}" )
        }

        def params = mergedWithDefaults
        params.secret = kubernetes.
            secrets().
            withName(secretRef.name as String).
            get().
            data.
            get(secretRef.key as String)

        def groovy = renderTemplate(this.createScript,  params)
        sendScript(groovy, jenkins)
    }

    String getCreateScriptFilename() {
        if (spec.containsKey('usernamePassword')) {
            return '/credentials/createUsernamePassword.groovy'
        }
        if (spec.containsKey('secretString')) {
            return '/credentials/createSecretString.groovy'
        }
        throw new IllegalArgumentException( "Unsupported credentials type: ${this}" )
    }

    @Override
    String getCreateScript() {
        fromClassPath( this.createScriptFilename )
    }
}
