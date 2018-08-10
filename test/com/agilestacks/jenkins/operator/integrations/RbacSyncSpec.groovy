package com.agilestacks.jenkins.operator.integrations

import com.agilestacks.jenkins.operator.kubernetes.KubernetesResourceController
import com.agilestacks.jenkins.operator.kubernetes.RateLimiter
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.server.mock.KubernetesServer
import io.fabric8.kubernetes.client.utils.Serialization
import spock.lang.Specification

class RbacSyncSpec extends Specification {
    final GITHUB_FILE = 'https://raw.githubusercontent.com/agilestacks/components/master/jenkins/rbac-operator.yaml.template'.toURL()
    final GITHUB_PARAMS = [
        requestProperties: [
            Authorization: "token ${ System.getenv("GITHUB_TOKEN") }",
            Accept: 'application/vnd.github.v3.raw',
        ],
        connectTimeout: 30000,
        readTimeout: 30000,
        useCaches: true,
        allowUserInteraction: false,
        followRedirects: true,
    ]

    KubernetesServer kubernetes
    KubernetesResourceController controller

    def "valid github integration"() {
        when:
        def url = 'https://api.github.com'.toURL()

        then:
        assert System.getenv('GITHUB_TOKEN'), 'envvar GITHUB_TOKEN must be configured'
        assert url.getText(GITHUB_PARAMS), 'GITHUB_TOKEN must be valid'
    }

    def "CRD must be in sync with RBAC"() {
        setup:
        def watched = []
        controller.watch(_) >> {
            watched << _
        }

        when:
        def content = GITHUB_FILE.getText(GITHUB_PARAMS)

        then:
        assert content, 'RBAC file must be downloadable'

        when:
        def rbac = Serialization.yamlMapper().readValue(content, Map)

        then:
        rbac.metadata.name == 'jenkins-operator'
        rbac.kind == 'ClusterRole'

        when:
        def resources = rbac.rules.find {
            'watch' in it.verbs && 'jenkins.agilestacks.com' in it.apiGroups
        }?.resources

        then:
        assert resources, 'ClusterRole must contain watch resources for api group jenkins.agilestacks.com'

    }

    def setup() {
        kubernetes = new KubernetesServer(https: true, curdMode: false)
        kubernetes.before()
        def client = kubernetes.client as DefaultKubernetesClient
        def queue = new RateLimiter()
        controller = new KubernetesResourceController(kubernetes: client, queue: queue)

        assert client
    }

    def cleanup() {
        kubernetes.after()
    }
}
