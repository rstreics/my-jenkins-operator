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
def allCreds = store.getCredentials(domain)

def client = new DefaultKubernetesClient(new ConfigBuilder().build())
if (client.masterUrl && client.namespace) {
  client.
    secrets().
    withLabels([
      'project': 'jenkins',
      'qualifier': 'credentials'
    ]).
    list().
    items.
    grep { it.data }.
    each { secret ->
      secret.data.keys.
        grep { credId -> allCreds.find{secret.id == credId} == null }.
        each { credId ->
          def val = secret.data[credId]
          // def creds = new StringCredentialsImpl(
          //                   CredentialsScope.GLOBAL,
          //                   credId,
          //                   credId,
          //                   new hudson.util.Secret( value ))
          def userPass = new UsernamePasswordCredentialsImpl(
                                CredentialsScope.GLOBAL, key, "", key, val )
          store.addCredentials(domain, userPass)
          store.addCredentials(domain, creds)
        }
    }
}

