package com.agilestacks.jenkins.operator.util

import com.agilestacks.jenkins.operator.jenkins.JenkinsHttpClient
import com.agilestacks.jenkins.share.StringReplace
import groovy.util.logging.Log
import io.fabric8.kubernetes.client.CustomResource
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import spock.lang.Specification

class ScriptableResourceSpec extends Specification {
    MockWebServer server
    JenkinsHttpClient client
    final static MAGIC_STRING = 'Status: CONVERGED <EOF>'
    final static INVALID_MAGIC_STRING = 'Status: SOMETHING ELSE <EOF>'

    def "can render script from classpath"() {
        given:
        def resource1 = new Dummy()
        def resource2 = new Dummy()
        when:
        resource1.metadata.name = 'Dummy'
        resource2.metadata.name = 'Stubby'
        then:
        resource1.createScript != null
        resource1.deleteScript != null
        resource2.createScript != null
        resource2.deleteScript != null
    }

    def "can submit create and delete scripts to Jenkins"() {
        given:
        def resource = new Dummy()
        server.enqueue(new MockResponse().setResponseCode(200).setBody("println '${MAGIC_STRING}'"))
        server.enqueue(new MockResponse().setResponseCode(200).setBody("println '${MAGIC_STRING}'"))
        when:
        def result1 = resource.create(client)
        def result2 = resource.delete(client)
        then:
        resource.createScript != null
        resource.deleteScript != null
        result1 =~ MAGIC_STRING
        result2 =~ MAGIC_STRING
    }

    def "send script should work if script contains a magic string"() {
        given:
        def resource = new Dummy()
        server.enqueue(new MockResponse().setResponseCode(200).setBody("println '${MAGIC_STRING}'"))
        when:
        def result = resource.sendScript(resource.createScript, client)
        then:
        result =~ MAGIC_STRING
    }

    def "send script should throw exception if invalid magic string"() {
        given:
        def resource = new Dummy()
        server.enqueue(new MockResponse().setResponseCode(200).setBody("println '${INVALID_MAGIC_STRING}'"))
        when:
        resource.create(client)
        resource.delete(client)
        then:
        thrown(RuntimeException)
    }

    def "format script should replace unrendered mustaches with empty string"() {
        given:
        def script = """\
                        final NAME           = '{{metadata.name}}'
                        final URL            = '{{spec.repositoryUrl}}'
                        final BRANCH_SPEC    = '{{spec.branchSpec}}'
                        final JENKINSFILE    = '{{spec.pipeline}}'
                        final CREDENTIALS_ID = '{{spec.credentialsId}}'
                        final FOLDER         = '{{spec.folder}}'"""
        def resource = new Dummy()
        resource.metadata.name = 'Dummy'
        when:
        String result = resource.renderTemplate(script)
        then:
        result =~ /''/
        !(result =~ StringReplace.MUSTACHE)
    }

    @Log
    class Dummy extends CustomResource implements ScriptableResource {
        final Definition definition = fromClassPath('/scriptableresource/definition.yaml') as Definition
        final String createScript = fromClassPath('/scriptableresource/create.groovy') as String
        final String deleteScript = fromClassPath('/scriptableresource/delete.groovy') as String
    }

    def setup() {
        server = new MockWebServer()
        server.start()
        client = new JenkinsHttpClient(server.url("/"))
    }

    def cleanup() {
        server.shutdown()
    }
}
