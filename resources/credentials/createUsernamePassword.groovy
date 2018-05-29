package credentials

import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.domains.Domain

def USERNAME    = '{{spec.usernamePassword.username}}' ?: null
def PASSWORD    = new String('{{secret}}'.decodeBase64())
def DESCRIPTION = '{{spec.description}}'?: null
def DOMAIN      = '{{spec.domain}}' ?: null
def CREDS_ID    = '{{spec.id}}' ?: '{{metadata.name}}' ?: null
def SCOPE       = '{{spec.scope}}' ?: 'GLOBAL'

def domain = Domain.global()
def domainCredsMap = SystemCredentialsProvider.instance.domainCredentialsMap

def scope = SCOPE.toUpperCase() as CredentialsScope

if (DOMAIN != 'global') {
    def domains = domainCredsMap.collectEntries { d, _ -> [ (d.name): d] }
    domain = domains[DOMAIN]
    if (!domain) {
        domain = new Domain(DOMAIN, "Created by operator", [])
        println "Creating a new domain ${DOMAIN}"
    } else {
        println "Found existing domain ${DOMAIN}"
    }
} else {
    println "Use global domain"
}

List existing = domainCredsMap[domain] ?: []
if ( !existing.find { it.id == CREDS_ID } ) {
    def creds = new UsernamePasswordCredentialsImpl(scope, CREDS_ID, DESCRIPTION, USERNAME, PASSWORD)
    SystemCredentialsProvider.instance.addCredentials(domain, creds)
} else {
    println "Existing creds ${CREDS_ID} has been found... doing nothing"
}

println 'Status: CONVERGED <EOF>'
