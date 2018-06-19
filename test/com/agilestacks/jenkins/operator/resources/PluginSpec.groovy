package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.BasicScriptableRoutines
import spock.lang.Specification

class PluginSpec extends Specification implements BasicScriptableRoutines<Plugin> {

    def "all plugin scripts accessible and has magic string"() {
        given:
        def resource = new Plugin()

        expect:
        resource.createScriptFile != null
        resource.deleteScriptFile != null
        resource.createScript != null
        resource.deleteScript != null
        resource.createScript =~ MAGIC_STRING
        resource.deleteScript =~ MAGIC_STRING
    }

}
