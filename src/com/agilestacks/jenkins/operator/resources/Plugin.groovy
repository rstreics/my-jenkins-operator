package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.ScriptableResource
import groovy.util.logging.Log
import io.fabric8.kubernetes.client.CustomResource

@Log
class Plugin extends CustomResource implements ScriptableResource {
    final Definition definition = fromClassPath('/plugin/definition.yaml') as Definition
    final String createScript = fromClassPath('/plugin/create.groovy')
    final String deleteScript = fromClassPath('/plugin/delete.groovy')

    final Map<String, ?> defaults = [
            udpateCenterUrl: 'https://TBD'
    ]
}
