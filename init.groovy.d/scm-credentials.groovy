#!/usr/bin/env groovy

import io.fabric8.kubernetes.client.*
import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import org.jenkinsci.plugins.plaincredentials.impl.*
import java.util.logging.Logger

def log = Logger.getLogger(this.class.name)
def store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
def domain = Domain.global()

def globalCreds = SystemCredentialsProvider
                       .instance
                       .getDomainCredentialsMap()[ (domain) ]
                       .collectEntries { [ (it.id):  it ] }

def client = new DefaultKubernetesClient(new ConfigBuilder().build())
if (!client.masterUrl || !client.namespace) {
  return
}

client
  .secrets()
  .withLabels([
    'project': 'jenkins',
    'qualifier': 'credentials'
  ])
  .list()
  .items
  .grep { it.data }
  .collect { it.data }
  .each {
    it.grep { !globalCreds.containsKey( it.key ) }
      .collect {
        def user = it.key
        def pass = new String(it.value.decodeBase64())
        new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, user, "", user, pass)
      }.each {
        store.addCredentials(domain, it)
      }
  }

