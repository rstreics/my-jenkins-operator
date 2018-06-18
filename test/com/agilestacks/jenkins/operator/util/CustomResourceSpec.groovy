package com.agilestacks.jenkins.operator.util

import com.agilestacks.jenkins.operator.resources.Credentials
import com.agilestacks.jenkins.operator.resources.EnvVars
import com.agilestacks.jenkins.operator.resources.Pipeline
import groovy.util.logging.Log
import com.agilestacks.jenkins.operator.util.ScriptableResource
import io.fabric8.kubernetes.client.utils.Serialization
import spock.lang.Specification

@Log
class CustomResourceSpec extends Specification {
    static final MAGIC_STRING = /(?i)\s*print(ln)?\s*[(]?\s*['"]Status:\s+CONVERGED\s*<EOF>\s*['"]\s*[)]?\s*[;]?\s*/

    def "pipeline custom resource scripts has magic string"() {
        given:
            def rsc = new Pipeline()
        expect:
            hasDefinitionAndScripts(rsc)
            rsc.createScript =~ MAGIC_STRING
            rsc.deleteScript =~ MAGIC_STRING
    }

    def "envvars custom resource scripts has magic string"() {
        given:
            def rsc = new EnvVars()
        expect:
            hasDefinitionAndScripts(rsc)
            rsc.createScript =~ MAGIC_STRING
            rsc.deleteScript =~ MAGIC_STRING
    }

    def "credentials custom resource scripts has magic string"() {
        given:
            def rsc1 = fromYaml('/credentials1.yaml', Credentials)
            def rsc2 = fromYaml('/credentials2.yaml', Credentials)
        expect:
            hasDefinitionAndScripts(rsc1)
            hasDefinitionAndScripts(rsc2)
            rsc1.createScriptFile == '/credentials/createSecretString.groovy'
            rsc2.createScriptFile == '/credentials/createUsernamePassword.groovy'
            rsc1.createScript =~ MAGIC_STRING
            rsc1.deleteScript =~ MAGIC_STRING
            rsc2.createScript =~ MAGIC_STRING
            rsc2.deleteScript =~ MAGIC_STRING
    }

    def hasDefinitionAndScripts(ScriptableResource rsc) {
        rsc.definition && rsc.createScript && rsc.deleteScript
    }

    def fromYaml(String filepath, Class<ScriptableResource> clazz) {
        final mapper = Serialization.yamlMapper()
        def text = this.class.getResourceAsStream(filepath).text
        mapper.readValue(text, clazz)
    }
}
