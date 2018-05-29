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
props.load(new StringReader( new String(VARS.decodeBase64()) ))

def jenk = Jenkins.get()

def envVars = jenk.globalNodeProperties.envVars?.get(0)
if (!envVars) {
    def envVarProps = new EnvironmentVariablesNodeProperty()
    jenk.globalNodeProperties.add( envVarProps )
    envVars = envVarProps.envVars
}

if (MERGE == 'ours') {
    def onlyNew = props.findAll { k, v ->
        println "Processing variable ${k}: ${v}"
        !envVars.containsKey(k)
    } as Map
    envVars.putAll(onlyNew)
} else if (MERGE == 'their') {
    envVars.overrideAll( props as Map )
} else {
    new RuntimeException("Unknown global Jenkins variables merge option: ${MERGE}")
}

jenk.save()

println 'Status: CONVERGED <EOF>'
