package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.ScriptableResource
import io.fabric8.kubernetes.client.CustomResource

class GithubOrganization extends CustomResource implements ScriptableResource {
    final String definitionFile   = '/githuborg/definition.yaml'
    final String createScriptFile = '/githuborg/create.groovy'
    final String deleteScriptFile = '/githuborg/delete.groovy'

    final Map<String, ?> defaults = [:]
}
