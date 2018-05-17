package com.agilestacks.jenkins.operator

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import spock.lang.Specification

import static com.agilestacks.jenkins.operator.JenkinsHttpClient.*

class JenkinsHttpClientSpec extends Specification {
    MockWebServer server
    JenkinsHttpClient client

    final SUCCESS = 'Status: CONVERGED <EOF>'
    final ERROR = 'Status: ERROR <EOF>'

    def "send ping request and receive version number"() {
        given:
            final version = 'fakeJenkinsVersion'
            server.enqueue(new MockResponse().addHeader('X-Jenkins', version))
        when:
            def resp = client.ping()
        then:
            version == resp
    }

    def "send script successfully"() {
        given:
            server.enqueue(new MockResponse().setBody(SUCCESS))

            def groovy = """
                println "Hello, World"
                println "${SUCCESS}"
            """.stripIndent().trim()
        when:
            def resp = client.postScript(groovy)
        then:
            resp =~ MAGIC_STRING
    }

    def "send script with http error"() {
        given:
            server.enqueue(new MockResponse().setResponseCode(404))
            def groovy = """
                println "Hello, World"
                println "${SUCCESS}"
            """.stripIndent().trim()
        when:
            client.postScript(groovy)

        then:
            thrown( ConnectException )
    }

    def "send script with script error"() {
        given:
            server.enqueue(new MockResponse().setBody(ERROR))

            def groovy = """
                println "Hello, World"
                println "${SUCCESS}"
            """.stripIndent().trim()
        when:
            client.postScript(groovy)

        then:
            thrown( RuntimeException )
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
