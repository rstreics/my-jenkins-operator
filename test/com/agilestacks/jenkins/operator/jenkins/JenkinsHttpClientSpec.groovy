package com.agilestacks.jenkins.operator.jenkins

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import spock.lang.Specification

class JenkinsHttpClientSpec extends Specification {
    MockWebServer server
    JenkinsHttpClient client

//    final SUCCESS = 'Status: CONVERGED <EOF>'
//    final ERROR = 'Status: ERROR <EOF>'

    def "send ping request and receive version number"() {
        given:
        final version = 'fakeJenkinsVersion'
        server.enqueue(new MockResponse().addHeader('X-Jenkins', version))
        when:
        def resp = client.ping()
        then:
        version == resp
    }

    def "initiate successful post request"() {
        given:
        server.enqueue(new MockResponse().setResponseCode(200).setBody('ok'))
        when:
        def resp = client.post('/script', [script: 'dummmy'])
        then:
        resp == 'ok'
    }

    def "initiate post requests and handle error"() {
        given:
        server.enqueue(new MockResponse().setResponseCode(404))
        when:
        client.post('/script', [script: 'dummmy'])
        then:
        thrown(ConnectException)
    }

//    def "correctly handle post request with error"() {
//        given:
//            kubernetes.enqueue(new MockResponse().setBody(ERROR))
//
//            def groovy = """
//                println "Hello, World"
//                println "${SUCCESS}"
//            """.stripIndent().trim()
//        when:
//            client.postScript(groovy)
//
//        then:
//            thrown( RuntimeException )
//    }

    def setup() {
        server = new MockWebServer()
        server.start()
        client = new JenkinsHttpClient(server.url("/"))
    }

    def cleanup() {
        server.shutdown()
    }

}
