package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.resources.EnvVars
import spock.lang.Specification

class EnvVarsSpec extends Specification {

    def "should merge option accessible from defaults"() {
        when:
            def resource = new EnvVars()
        then:
            resource.mergedWithDefaults.spec.merge == 'ours'
    }


    def "should defaults should be overriden"() {
        given:
            def resource = new EnvVars()
        when:
            resource.spec.merge = 'their'
        then:
            resource.mergedWithDefaults.spec.merge == 'their'
    }
}
