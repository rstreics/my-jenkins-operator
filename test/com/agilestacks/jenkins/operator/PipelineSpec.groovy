package com.agilestacks.jenkins.operator

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionListBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.server.mock.KubernetesServer
import spock.lang.*

class PipelineSpec extends Specification {

    KubernetesServer server
    KubernetesClient client
    PipelineController controller

    def "list CRDs"() {
        given:
        server \
                .expect()
                .get()
                .withPath('/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions')
                .andReturn(200, testCrdListWithPipeline() ).always()

        expect:
            def result = client.customResourceDefinitions().list()
            1 == result.items.size()
    }

    def "pipeline controller creates new CRD if needed"() {
        given:
        server \
                .expect()
                .get()
                .withPath('/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions')
                .andReturn(200, emptyCrdList() ).always()
        when:
        controller.registerCrd()

        then:
        def result = client.customResourceDefinitions().list()
        1 == result.items.size()
    }

    def testCrdListWithPipeline() {
        new CustomResourceDefinitionListBuilder()
                .addNewItem()
                .withNewMetadata()
                .withName(controller.CRD)
                .and().and()
                .build()
    }

    def emptyCrdList() {
        new CustomResourceDefinitionListBuilder().build()
    }

    def setup() {
        server = new KubernetesServer()
        server.before()
        client = server.client
        controller = new PipelineController( kubernetes: client )
    }

    def cleanup() {
        server.after()
    }
}