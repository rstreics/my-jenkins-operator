#!/usr/bin/env groovy
import io.fabric8.kubernetes.api.model.ConfigMap
import jenkins.model.*
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
        ]).list().items[0]?.data

//if (!configmap) {
//    println 'Cannot find slack configmap'
//    return
//}

def slackParameters = [
        slackBaseUrl:             configmap?.slackBaseUrl ?: '',
        slackBotUser:             'true',
        slackBuildServerUrl:      configmap?.slackBuildServerUrl ?: '',
        slackRoom:                configmap?.slackRoom ?: '',
        slackSendAs:              configmap?.slackSendAs ?: 'Jenkins',
        slackTeamDomain:          configmap?.slackTeamDomain ?: '',
        slackToken:               '',
        slackTokenCredentialId:   ''
]

Jenkins jenk = Jenkins.getInstance()
final slack = jenk.getExtensionList(jenkins.plugins.slack.SlackNotifier.DescriptorImpl.class)[0]

JSONObject formData = ['slack': ['tokenCredentialId': '']]
if (configmap?.slackToken) {
    final secretText = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        'slack-token',
        'Slack Jenkins integration token',
        Secret.fromString(configmap?.slackToken)
    )
    slackParameters['slackParameters'] = secretText.id

    final domain = Domain.global()
    final store = jenk.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
    store.addCredentials(domain, secretText)
    formData.slack.tokenCredentialId = secretText.id
}

def request = [getParameter: { name -> slackParameters[name] }] as org.kohsuke.stapler.StaplerRequest
slack.configure(request, formData)

// save to disk
slack.save()
jenk.save()
