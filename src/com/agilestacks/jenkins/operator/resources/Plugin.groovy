package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.ScriptableResource
import groovy.util.logging.Log
import io.fabric8.kubernetes.client.CustomResource

@Log
class Plugin extends CustomResource implements ScriptableResource {
    final String definitionFile   = '/plugin/definition.yaml'
    final String createScriptFile = '/plugin/create.groovy'
    final String deleteScriptFile = '/plugin/delete.groovy'

    final Map<String, ?> defaults = [
            udpateCenterUrl: 'https://TBD'
    ]
}
