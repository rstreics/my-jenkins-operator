package com.agilestacks.jenkins.operator

import com.agilestacks.jenkins.operator.crd.ScriptableResource
import io.fabric8.kubernetes.client.CustomResource

class EnvVars extends CustomResource implements ScriptableResource {
    final String definitionFile   = '/envvars/definition.yaml'
    final String createScriptFile = '/envvars/create.groovy'
    final String deleteScriptFile = '/envvars/delete.groovy'

    Map<String, ?> defaults = [ merge: 'ours' ]

    @Override
    Map<String, ?> getMergedWithDefaults() {
        def vars1 = defaults.variables ?: [:]
        def vars2 = spec.variables ?: [:]
        def props = toPropertiesString( vars1 + vars2 )
        [ kind           : kind,
          apiVersion     : apiVersion,
          spec           : getDefaults() + getSpec(),
          metadata       : metadata.properties,
          variablesBase64: props.bytes.encodeBase64() ]
    }

    static String toPropertiesString(arg) {
        def props = new Properties( arg as Map )
        def writer = new StringWriter()
        props.store(new PrintWriter(writer),null)
        writer.buffer.toString()
    }
}
