package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.BasicScriptableRoutines
import spock.lang.Specification

class CredentialsSpec extends Specification implements BasicScriptableRoutines<Credentials> {

    def "username and password credentials scripts should be accessible and contain magic string"() {
        given:
        def resource = fromYaml('/credentials1.yaml', Credentials)

        expect:
        resource.spec.containsKey('usernamePassword')
        resource.createScriptFile == '/credentials/createUsernamePassword.groovy'
        resource.deleteScriptFile != null
        resource.createScript =~ MAGIC_STRING
        resource.deleteScript =~ MAGIC_STRING
    }


    def "secret string credentials scripts should be accessible and contain magic string"() {
        given:
        def resource = fromYaml('/credentials2.yaml', Credentials)

        expect:
        resource.spec.containsKey('secretString')
        resource.createScriptFile == '/credentials/createSecretString.groovy'
        resource.deleteScriptFile != null
        resource.createScript =~ MAGIC_STRING
        resource.deleteScript =~ MAGIC_STRING
    }
}
