package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.ScriptableResource
import groovy.util.logging.Log
import io.fabric8.kubernetes.client.CustomResource

@Log
class Kubernetes extends CustomResource implements ScriptableResource {
    final Definition definition = fromClassPath('/kubernetees/definition.yaml') as Definition
    final String createScript   = fromClassPath('/kubernentes/create.groovy')
    final String deleteScript   = fromClassPath('/kubernentes/delete.groovy')

}
