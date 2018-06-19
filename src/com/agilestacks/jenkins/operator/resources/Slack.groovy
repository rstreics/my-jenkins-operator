package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.ScriptableResource
import io.fabric8.kubernetes.client.CustomResource

class Slack extends CustomResource implements ScriptableResource {
    final String definitionFile   = '/slack/definition.yaml'
    final String createScriptFile = '/slack/create.groovy'
    final String deleteScriptFile = '/slack/delete.groovy'
}
