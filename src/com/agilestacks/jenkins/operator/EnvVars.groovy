package com.agilestacks.jenkins.operator

import com.agilestacks.jenkins.operator.crd.ScriptableResource
import com.agilestacks.jenkins.operator.crd.Status
import io.fabric8.kubernetes.client.CustomResource

class EnvVars extends CustomResource implements ScriptableResource, Status {
    final String definitionFile   = '/envvars/definition.yaml'
    final String createScriptFile = '/envvars/create.groovy'
    final String deleteScriptFile = '/envvars/delete.groovy'

    Map<String, ?> defaults = [ merge: 'ours' ]
    @Lazy
    Map<String, ?> scriptParameters = [kind          : kind,
                                       apiVersion    : apiVersion,
                                       spec          : getSpec(),
                                       metadata      : metadata.properties,
                                       specPropertiesBase64: toPropertiesString().bytes.encodeBase64()]

    def toPropertiesString() {
        def props = new Properties(spec)
        def writer = new StringWriter()
        props.store(new PrintWriter(writer),null)
        writer.buffer.toString()
    }
}
