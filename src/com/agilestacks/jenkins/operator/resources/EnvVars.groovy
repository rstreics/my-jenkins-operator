package com.agilestacks.jenkins.operator.resources

import groovy.util.logging.Log
import com.agilestacks.jenkins.operator.util.ScriptableResource
import io.fabric8.kubernetes.client.CustomResource

@Log
class EnvVars extends CustomResource implements ScriptableResource {
    final Definition definition = fromClassPath('/envvars/definition.yaml') as Definition
    final String createScript   = fromClassPath('/envvars/create.groovy')
    final String deleteScript   = fromClassPath('/envvars/delete.groovy')

    final Map<String, ?> defaults = [ merge: 'ours' ]

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
