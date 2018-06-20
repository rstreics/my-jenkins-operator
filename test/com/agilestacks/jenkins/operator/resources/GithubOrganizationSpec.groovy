package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.BasicScriptableRoutines
import spock.lang.Specification

class GithubOrganizationSpec extends Specification implements BasicScriptableRoutines<GithubServer> {

    def "all github organiztion custom resource scripts has magic string"() {
        given:
        def resource = new GithubServer()

        expect:
        resource.createScript =~ MAGIC_STRING
        resource.deleteScript =~ MAGIC_STRING

    }

    def "github org resource has defaults"() {
        given:
        def resource1 = new GithubServer()
        def resource2 = new GithubServer()
        resource2.spec.apiUrl = 'https://www.google.com'
        resource2.spec.manageHooks = false

        expect:
        resource1.mergedWithDefaults.spec.apiUrl == 'https://api.github.com'
        resource2.mergedWithDefaults.spec.apiUrl == 'https://www.google.com'
        resource1.mergedWithDefaults.spec.manageHooks == true
        resource2.mergedWithDefaults.spec.manageHooks == false
    }
}
