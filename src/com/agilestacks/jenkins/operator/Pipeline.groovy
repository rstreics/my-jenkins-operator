package com.agilestacks.jenkins.operator

import io.fabric8.kubernetes.client.CustomResource

class Pipeline extends CustomResource implements JenkinsCustomResource {
    final String definitionCPRef   = '/pipeline/definition.yaml'
    final String createScriptCPRef = '/pipeline/create.groovy'
    final String deleteScriptCPRef = '/pipeline/delete.groovy'
}
