#!/usr/bin/env groovy
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import io.fabric8.kubernetes.client.*
import java.util.logging.Logger
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval

def log = Logger.getLogger(this.class.name)
def client = new DefaultKubernetesClient(new ConfigBuilder().build())
if (!client.masterUrl || !client.namespace) {
  return
}

def approvals = ScriptApproval.get()

client
  .configMaps()
  .withLabels([ project: 'jenkins',
                qualifier: 'script-approvals'])
  .list().items.data
  .flatten().collect {
    if (it in Map) {
      return it.values()
    }
    if (it in String) {
      return it.readLines()
    }
    return it
  }
  .flatten().collect {
    // not duplication
    if (it in Map) {
      return it.values()
    }
    if (it in String) {
      return it.readLines()
    }
    return it
  }
  .flatten().unique().each {
    log.info "Auto-approving: ${it}"
    approvals.approveSignature( it )
  }

