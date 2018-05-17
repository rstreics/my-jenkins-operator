package com.agilestacks.jenkins.operator

import io.fabric8.kubernetes.client.CustomResource

class Variables extends CustomResource implements JenkinsCustomResource{
    final String definitionCPRef   = '/vars/definition.yaml'
    final String createScriptCPRef = '/vars/create.groovy'
    final String deleteScriptCPRef = '/vars/delete.groovy'
}
