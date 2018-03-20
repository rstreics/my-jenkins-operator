#!/usr/bin/env groovy
// @GrabResolver(name='releases', root='http://repo.jenkins-ci.org/releases/')
// @Grab(group='org.jenkins-ci.plugins', module='github-branch-source', version='2.3.2')
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import io.fabric8.kubernetes.client.*
import java.util.logging.Logger
import org.jenkinsci.plugins.github.*
import org.jenkinsci.plugins.github.config.*
import jenkins.branch.*
import org.jenkinsci.plugins.github_branch_source.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.jenkins.GitHubWebHook

def log = Logger.getLogger(this.class.name)
def client = new DefaultKubernetesClient(new ConfigBuilder().build())
if (!client.masterUrl || !client.namespace) {
  return
}

def configs = client.
                configMaps().
                withLabels([
                  project: 'jenkins',
                  qualifier: 'github-org'
                ]).
                list().
                items.
                collect {
                  [ name:   it.metadata.name,
                    apiUrl: it.data.apiUrl ?: GitHubServerConfig.GITHUB_URL,
                    manageHooks: it.data.get('manageHooks', true),
                    username: it.data.username ?: it.data.organization,
                  ] + it.data
                }

def credsProvider = new GitHubTokenCredentialsCreator()
def storedCredentials = SystemCredentialsProvider.
                          instance.
                          getDomainCredentialsMap().
                          collectEntries { domain, creds ->
                            [ (domain.name):  creds[0] ]
                          }

def github = GitHubPlugin.configuration()

def serverNames = github.configs.collect{ it.name }
github.configs += configs
                    .grep { !(it.name in serverNames) }
                    .collect {
  def urlHost   = new java.net.URI( it.apiUrl as String ).host
  def creds     = storedCredentials[urlHost] ?: credsProvider.createCredentials(it.apiUrl, it.deployment_key, it.name)
  def server    = new GitHubServerConfig( creds.id )
  server.name   = it.name
  server.apiUrl = it.apiUrl
  server.manageHooks = it.manageHooks
  if (!github.hookSecretConfig
    && server.manageHooks
    && creds) {
    github.hookSecretConfig = new HookSecretConfig( creds.id )
  }
  log.info "Added Github server ${server.name}: ${server.apiUrl}"
  server
}
github.save()

def store = Jenkins
              .instance
              .getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0]
              .store

def existingCreds = SystemCredentialsProvider
                      .instance
                      .getDomainCredentialsMap()[ (Domain.global()) ]
                      .collect { it.id }
configs
  .grep { it.username && it.deployment_key }
  .grep { !(it.username in existingCreds) }
  .each {
    def userPass = new UsernamePasswordCredentialsImpl(
      CredentialsScope.GLOBAL,
      it.username, "Github ${it.organization} deployment key",
      it.username,
      it.deployment_key
    )
    log.info "Store Github credentials ${it.username}"
    store.addCredentials(Domain.global(), userPass)
  }


def ofs = Jenkins.instance.getAllItems(OrganizationFolder)
def existingOrgs = ofs.collect { it.name }
configs
  .grep { it.organization }
  .grep { !(it.organization in existingOrgs) }
  .each {
    log.info "Apply Github org folder ${it.organization}"

    def nav = new GitHubSCMNavigator(it.apiUrl, it.organization, it.username, 'SAME')
    nav.includes = it.includes
    nav.excludes = it.excludes

    def gh = Jenkins.instance.createProject(OrganizationFolder, it.organization)
    gh.description = "${it.organization}, Github Organization"
    gh.navigators << nav
    gh.scheduleBuild(0, new BranchIndexingCause())
  }

GitHubWebHook.get().reRegisterAllHooks();
Jenkins.instance.save()

