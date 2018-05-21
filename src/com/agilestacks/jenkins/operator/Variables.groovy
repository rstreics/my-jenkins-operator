package com.agilestacks.jenkins.operator

import com.agilestacks.jenkins.operator.crd.ScriptableResource
import com.agilestacks.jenkins.operator.crd.Status
import io.fabric8.kubernetes.client.CustomResource

class Variables extends CustomResource implements ScriptableResource, Status {
    final String definitionFile   = '/vars/definition.yaml'
    final String createScriptFile = '/vars/create.groovy'
    final String deleteScriptFile = '/vars/delete.groovy'
}
