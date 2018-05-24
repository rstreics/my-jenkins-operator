package envvars

import hudson.slaves.EnvironmentVariablesNodeProperty
import jenkins.model.*
import java.util.logging.Logger
import java.io.*

def VARS = """\
{{specProperties}}
"""

def MERGE = '{{spec.merge}}' ?: 'ours'

def props = new Properties()
props.load(new StringReader( VARS ))

def jenk = Jenkins.get()
def globalProps = jenk.globalNodeProperties
def envVarsNodePropertyList = globalProps?.envVars

def envVars
if (envVarsNodePropertyList == null || envVarsNodePropertyList.empty) {
    def envVarProps = new EnvironmentVariablesNodeProperty()
    globalProps.add( envVarProps )
    envVars = envVarProps.envVars
} else {
    envVars = envVarsNodePropertyList.first
}

if (MERGE == 'ours') {
    def onlyNew = props.findAll { k, v -> !envVars.containsKey(k) } as Map<? extends String, ? extends String>
    envVars.putAll(onlyNew)
} else if (MERGE == 'their') {
    envVars.putAll(props)
} else {
    new RuntimeException("Unknown global Jenkins variables merge option: ${MERGE}")
}

jenk.save()

println 'Status: CONVERGED <EOF>'
