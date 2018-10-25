package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.ScriptableResource
import groovy.util.logging.Log
import io.fabric8.kubernetes.client.CustomResource

@Log
class GithubOrganization extends CustomResource implements ScriptableResource {
    final Definition definition = fromClassPath('/githuborg/definition.yaml') as Definition
    final String createScript   = fromClassPath('/githuborg/create.groovy')
    final String deleteScript   = fromClassPath('/githuborg/delete.groovy')

    final Map defaults = [:]
}
