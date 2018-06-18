package githuborg

import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import java.util.logging.Logger
import org.jenkinsci.plugins.github.*
import org.jenkinsci.plugins.github.config.*

def log = Logger.getLogger(this.class.name)

final NAME = '{{metadata.name}}' ?: null

def github = GitHubPlugin.configuration()
if ( github.configs.removeAll { it.name == NAME } ) {
    println "Deleted Github Server ${NAME}"
} else {
    println "WARN: Github Server ${NAME} has not been found"
}

github.save()
Jenkins.get().save()

println 'Status: CONVERGED <EOF>'
