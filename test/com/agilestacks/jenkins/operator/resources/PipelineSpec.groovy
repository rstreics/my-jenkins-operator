package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.BasicScriptableRoutines
import groovy.json.JsonSlurper
import spock.lang.Specification

class PipelineSpec extends Specification implements BasicScriptableRoutines<Credentials> {

    def "all scripts accessible and contains a magic string"() {
        given:
        final resource = new Pipeline()

        expect:
        resource.createScript =~ MAGIC_STRING
        resource.deleteScript =~ MAGIC_STRING
    }


    def "can add new parameters"() {
        given:
        final resource1 = new Pipeline()

        when:
        resource1.addParameter('TEST', 'passed')

        then:
        resource1.spec.parameters.size == 1
        resource1.spec.parameters[0].name == 'TEST'
        resource1.spec.parameters[0].defaultValue == 'passed'
    }


    def "parameters has been encoded base64"() {
        given:
        final resource1 = new Pipeline()

        when:
        resource1.addParameter('TEST', 'passed')
        final base64Encoded = resource1.mergedWithDefaults.paramsBase64 as String
        final params1 = resource1.spec.parameters
        final params2 = new JsonSlurper().parse( base64Encoded.decodeBase64() )

        then:
        params1 == params2
    }
}
