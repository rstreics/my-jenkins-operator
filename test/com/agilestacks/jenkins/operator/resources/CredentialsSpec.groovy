package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.BasicScriptableRoutines
import spock.lang.Specification

class CredentialsSpec extends Specification implements BasicScriptableRoutines<Credentials> {

    def "custom resource scripts should be accessible and contains magic string"() {
        given:
        def resource1 = fromYaml('/credentials1.yaml', Credentials)
        def resource2 = fromYaml('/credentials2.yaml', Credentials)

        expect:
        resource1.createScriptFile == '/credentials/createUsernamePassword.groovy'
        resource1.deleteScriptFile != null
        resource1.createScript =~ MAGIC_STRING
        resource1.deleteScript =~ MAGIC_STRING

        resource2.createScriptFile == '/credentials/createSecretString.groovy'
        resource2.createScript =~ MAGIC_STRING
    }
}
