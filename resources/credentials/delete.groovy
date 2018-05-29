package credentials

import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.domains.Domain

def DOMAIN      = '{{spec.domain}}' ?: null
def CREDS_ID    = '{{spec.id}}' ?: '{{metadata.name}}' ?: null
def SCOPE       = '{{spec.scope}}' ?: 'GLOBAL'

def scope = SCOPE.toUpperCase() as CredentialsScope

def creds = SystemCredentialsProvider.instance
def domain = Domain.global()
if (DOMAIN != 'global') {
    domain = creds.
                domainCredentialsMap.
                keySet().
                find {it.name == DOMAIN }
    if (!domain) {
        throw new IllegalArgumentException("Domain ${DOMAIN} has not been found")
    }
} else {
    println "Use global domain"
}

def existing = creds.domainCredentialsMap.get(domain)?.find {it.id == CREDS_ID}
if (!existing) {
    throw new IllegalArgumentException("Credentials ${CREDS_ID}@${DOMAIN} has not been found")
}

creds.removeCredentials(domain, existing)

println 'Not yet implemented'
println 'Status: CONVERGED <EOF>'
