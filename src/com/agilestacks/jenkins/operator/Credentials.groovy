package com.agilestacks.jenkins.operator

import com.agilestacks.jenkins.operator.crd.ScriptableResource
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.client.KubernetesClient

class Credentials extends CustomResource implements ScriptableResource {
    final String definitionFile   = '/credentials/definition.yaml'
    final String deleteScriptFile = '/credentials/delete.groovy'

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

        def groovy = formatGroovyScriptFromClasspath( createScriptFile,  params)
        sendScript(groovy, jenkins)
    }

    @Override
    String getCreateScriptFile() {
        if (spec.containsKey('secretString')) {
            return '/credentials/createUsernamePassword.groovy'
        } else if (spec.containsKey('usernamePassword')) {
            return '/credentials/createSecretString.groovy'
        }
        throw new IllegalArgumentException( "Unsupported credentials type: ${this}" )
    }
}
