package credentials

import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.domains.Domain

def SECRET_NAME = '{{spec.secretString.secretKeyRef.name}}'?: null
def SECRET_KEY  = '{{spec.secretString.secretKeyRef.key}}'?: null
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

def client = new DefaultKubernetesClient(new ConfigBuilder().build())

def secret = client.secrets().withName(SECRET_NAME).get()
def password = new String( secret.data.get( SECRET_KEY ).decodeBase64() )

List existing = domainCredsMap[domain] ?: []
if ( !existing.find { it.id == CREDS_ID } ) {
    def creds = new StringCredentialsImpl(scope, CREDS_ID, DESCRIPTION, USERNAME, password)
    SystemCredentialsProvider.instance.addCredentials(domain, creds)
} else {
    println "Existing creds ${CREDS_ID} has been found... doing nothing"
}

println 'Status: CONVERGED <EOF>'
