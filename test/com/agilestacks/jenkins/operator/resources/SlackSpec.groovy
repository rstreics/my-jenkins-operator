package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.BasicScriptableRoutines
import spock.lang.Specification

class SlackSpec extends Specification implements BasicScriptableRoutines<Slack> {
    def "slack custom resource scripts accessible and has magic string"() {
        given:
        def resource = new Slack()

        expect:
        resource.createScriptFile != null
        resource.deleteScriptFile != null
        resource.createScript != null
        resource.deleteScript != null
        resource.createScript =~ MAGIC_STRING
        resource.deleteScript =~ MAGIC_STRING
    }
}
