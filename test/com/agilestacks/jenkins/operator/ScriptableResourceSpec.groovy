package com.agilestacks.jenkins.operator

import com.agilestacks.jenkins.operator.crd.ScriptableResource
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
            def resource1 = new DummyCustomResource()
            def resource2 = new DummyCustomResource()
        when:
            resource1.metadata.name = 'Dummy'
            resource2.metadata.name = 'Stubby'
        then:
            resource1.createScript =~ 'Dummy'
            resource1.deleteScript =~ 'Dummy'
            resource2.createScript =~ 'Stubby'
            resource2.deleteScript =~ 'Stubby'
    }

    def "can submit create and delete scripts to Jenkins"() {
        given:
            def resource = new DummyCustomResource()
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
            def resource = new DummyCustomResource()
            server.enqueue(new MockResponse().setResponseCode(200).setBody("println '${MAGIC_STRING}'"))
        when:
            def result = resource.sendScript(resource.createScript, client)
        then:
            result =~ MAGIC_STRING
    }

    def "send script should throw exception if invalid magic string"() {
        given:
            def resource = new DummyCustomResource()
            server.enqueue(new MockResponse().setResponseCode(200).setBody("println '${INVALID_MAGIC_STRING}'"))
        when:
            resource.create(client)
            resource.delete(client)
        then:
            thrown(RuntimeException)
    }


    class DummyCustomResource extends CustomResource implements ScriptableResource {
        String definitionFile = '/pipeline/definition.yaml'
        String createScriptFile = '/scriptableresource/create.groovy'
        String deleteScriptFile = '/scriptableresource/delete.groovy'
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
