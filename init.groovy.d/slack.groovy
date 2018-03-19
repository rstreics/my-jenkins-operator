#!/usr/bin/env groovy
import io.fabric8.kubernetes.api.model.ConfigMap
import jenkins.model.*
import java.util.logging.Logger
import java.io.*

import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.client.*

import com.cloudbees.jenkins.plugins.sshcredentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.*
import hudson.util.Secret
import java.nio.file.Files
import jenkins.model.Jenkins
import net.sf.json.JSONObject
import org.jenkinsci.plugins.plaincredentials.impl.*
import java.util.logging.Logger

def log = Logger.getLogger(this.class.name)

def client = new DefaultKubernetesClient(new ConfigBuilder().build())
if (!client.masterUrl || !client.namespace) {
    println 'Cannot detect Kubernetes nature'
    return
}

def configmap = client.
        configMaps().
        withLabels([
                project: 'jenkins',
                qualifier: 'slack'
        ]).list().items.first()?.data

println configmap

if (!configmap) {
    println 'Cannot find slack configmap'
    return
}

def slackCredentialParameters = [
        description:  'Slack Jenkins integration token',
        id:           'slack-token',
        secret:       configmap.slackToken
]

def slackParameters = [
        slackBaseUrl:             configmap.slackBaseUrl,
        slackBotUser:             'true',
        slackBuildServerUrl:      configmap.slackBuildServerUrl,
        slackRoom:                configmap.slackRoom ?: '#jenkins',
        slackSendAs:              configmap.slackSendAs ?: 'Jenkins',
        slackTeamDomain:          configmap.slackTeamDomain,
        slackToken:               '',
        slackTokenCredentialId:   'slack-token'
]

Jenkins jenk = Jenkins.getInstance()
def domain = Domain.global()
def store = jenk.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
def slack = jenk.getExtensionList(jenkins.plugins.slack.SlackNotifier.DescriptorImpl.class)[0]
def secretText = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        slackCredentialParameters.id,
        slackCredentialParameters.description,
        Secret.fromString(slackCredentialParameters.secret)
)

JSONObject formData = ['slack': ['tokenCredentialId': 'slack-token']] as JSONObject
def request = [getParameter: { name -> slackParameters[name] }] as org.kohsuke.stapler.StaplerRequest
store.addCredentials(domain, secretText)
slack.configure(request, formData)

// save to disk
slack.save()
jenk.save()
