package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.ScriptableResource
import groovy.util.logging.Log
import io.fabric8.kubernetes.client.CustomResource

@Log
class GithubServer extends CustomResource implements ScriptableResource {
    final String definitionFile   = '/githubserver/definition.yaml'
    final String createScriptFile = '/githubserver/create.groovy'
    final String deleteScriptFile = '/githubserver/delete.groovy'

    final Map<String, ?> defaults = [
        apiUrl:'https://api.github.com',
        manageHooks: true,
    ]
}
