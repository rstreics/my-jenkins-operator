package githuborg

import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import java.util.logging.Logger
import org.jenkinsci.plugins.github.*
import org.jenkinsci.plugins.github.config.*
import com.cloudbees.jenkins.GitHubWebHook

def log = Logger.getLogger(this.class.name)

final NAME = '{{metadata.name}}' ?: null
final API_URL        = '{{spec.apiUrl}}' ?: null
final CREDENTIALS_ID = '{{spec.credentialsId}}' ?: null
final MANAGE_HOOKS   = '{{spec.manageHooks}}' ?: null

def github = GitHubPlugin.configuration()

if ( github.configs.find { it.name == NAME } ) {
    println 'GitHub ${NAME} already registered! Moving on'
    return
}

def server    = new GitHubServerConfig( CREDENTIALS_ID )
server.name   = NAME
server.apiUrl = API_URL
server.manageHooks = MANAGE_HOOKS
if (!github.hookSecretConfig
    && server.manageHooks
    && creds) {
    github.hookSecretConfig = new HookSecretConfig( CREDENTIALS_ID )
}
log.info "Added Github server ${server.name}: ${server.apiUrl}"
github.configs << server
github.save()
Jenkins.get().save()

println 'Status: CONVERGED <EOF>'
