package envvars

import hudson.slaves.EnvironmentVariablesNodeProperty
import jenkins.model.*
import java.util.logging.Logger
import java.io.*

def VARS = """\
{{variablesBase64}}
"""

def MERGE = '{{spec.merge}}' ?: null

def props = new Properties()
props.load(new StringReader( new String(VARS.decodeBase64()).trim() ))

def jenk = Jenkins.get()

def envVars = jenk.globalNodeProperties.envVars?.get(0)
if (!envVars) {
    def envVarProps = new EnvironmentVariablesNodeProperty()
    jenk.globalNodeProperties.add( envVarProps )
    envVars = envVarProps.envVars
}

props.each {k,v ->
    envVars.remove(k)
}

jenk.save()

println 'Status: CONVERGED <EOF>'
