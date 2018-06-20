package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.ScriptableResource
import groovy.util.logging.Log
import io.fabric8.kubernetes.client.CustomResource

@Log
class Slack extends CustomResource implements ScriptableResource {
    final Definition definition = fromClassPath('/slack/definition.yaml') as Definition
    final String createScript   = fromClassPath('/slack/create.groovy')
    final String deleteScript   = fromClassPath('/slack/delete.groovy')
}
