package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.BasicScriptableRoutines
import spock.lang.Specification

class EnvVarsSpec extends Specification implements BasicScriptableRoutines<EnvVars> {

    def "all scripts accessible and contains a magic string"() {
        given:
        def resource = new EnvVars()

        expect:
        resource.createScriptFile != null
        resource.deleteScriptFile != null
        resource.createScript != null
        resource.deleteScript != null
        resource.createScript =~ MAGIC_STRING
        resource.deleteScript =~ MAGIC_STRING
    }


    def "defaults may be overriden"() {
        given:
        def resource1 = new EnvVars()
        def resource2 = new EnvVars()
        when:
        resource2.spec.merge = 'their'

        then:
        resource1.mergedWithDefaults.spec.merge == 'ours'
        resource2.mergedWithDefaults.spec.merge == 'their'
    }


    def "contains envvars as properties encoded base64"() {
        given:
        def resource = fromYaml('/envVars1.yaml', EnvVars)

        when:
        def encoded = resource.getMergedWithDefaults().variablesBase64 as String
        def props = new Properties()
        props.load(new ByteArrayInputStream( encoded.decodeBase64() ))

        then:
        props.URL_VAR == 'https://google.com'
        props.STRING_VAR == 'hello, world!'
    }
}
