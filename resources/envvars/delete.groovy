package envvars

import hudson.slaves.EnvironmentVariablesNodeProperty
import jenkins.model.*
import java.util.logging.Logger
import java.io.*

def vars = """\
{{specAsProperties}}
"""

def props = new Properties()
props.load(new StringReader( vars ))

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

props.each {k,v ->
    envVars.remove(k)
}

jenk.save()

println 'Status: CONVERGED <EOF>'
