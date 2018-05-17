package vars

import hudson.slaves.EnvironmentVariablesNodeProperty
import jenkins.model.*
import java.util.logging.Logger
import java.io.*

def jenk = Jenkins.get()
def globalProps = jenk.globalNodeProperties
def envVarsNodePropertyList = globalProps?.envVars

def envVars
if (envVarsNodePropertyList == null || envVarsNodePropertyList.empty) {
    def envVarProps = new EnvironmentVariablesNodeProperty()
    globalProps.add( envVarProps )
    envVars = envVarProps.envVars
} else {
    envVars = envVarsNodePropertyList[0]
}

envVars['{{VARIABLE}}'] = '{{VALUE}}'

jenk.save()

println 'Status: CONVERGED <EOF>'
