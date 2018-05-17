package com.agilestacks.jenkins.operator

import spock.lang.Specification

class CustomResourceSpec extends Specification {
    static final MAGIC_STRING = /(?i)\s*print(ln)?\s*[(]?\s*['"]Status:\s+CONVERGED\s*<EOF>\s*['"]\s*[)]?\s*[;]?\s*/

    def "pipeline custom resource has been set up correctly"() {
        given:
            def resource = new Pipeline()
        expect:
            validCustomResource(resource)
            resource.createScript =~ MAGIC_STRING
            resource.deleteScript =~ MAGIC_STRING
    }

    def "vars custom resource has been set up correctly"() {
        given:
            def resource = new Variables()
        expect:
            validCustomResource(resource)
            resource.createScript =~ MAGIC_STRING
            resource.deleteScript =~ MAGIC_STRING
    }

    def validCustomResource(JenkinsCustomResource resource) {
        resource.definition &&
                resource.createScript &&
                resource.deleteScript
    }
}
