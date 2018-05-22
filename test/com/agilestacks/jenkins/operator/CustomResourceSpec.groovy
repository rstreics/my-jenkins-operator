package com.agilestacks.jenkins.operator

import com.agilestacks.jenkins.operator.crd.ScriptableResource
import spock.lang.Specification

class CustomResourceSpec extends Specification {
    static final MAGIC_STRING = /(?i)\s*print(ln)?\s*[(]?\s*['"]Status:\s+CONVERGED\s*<EOF>\s*['"]\s*[)]?\s*[;]?\s*/

    def "pipeline custom resource scripts has magic string"() {
        given:
            def rsc = new Pipeline()
        expect:
            validCustomResource(rsc)
            rsc.createScript =~ MAGIC_STRING
            rsc.deleteScript =~ MAGIC_STRING
    }

    def "vars custom resource scripts has magic string"() {
        given:
            def rsc = new Variables()
        expect:
            validCustomResource(rsc)
            rsc.createScript =~ MAGIC_STRING
            rsc.deleteScript =~ MAGIC_STRING
    }

    def validCustomResource(ScriptableResource rsc) {
        rsc.definition && rsc.createScript && rsc.deleteScript
    }
}