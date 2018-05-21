package com.agilestacks.jenkins.operator

import com.agilestacks.jenkins.operator.crd.KubernetesResourceController
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionListBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.server.mock.KubernetesServer
import spock.lang.*

class PipelineSpec extends Specification {

    KubernetesServer server
    KubernetesClient client
    KubernetesResourceController controller

    def "list CRDs"() {
        given:
        server \
                .expect()
                .get()
                .withPath('/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions')
                .andReturn(200, singlePipelineCrdList() ).always()
        when:
        controller.apply(new Pipeline().definition)

        then:
            def result = client.customResourceDefinitions().list()
            1 == result.items.size()
    }

    def "controller creates new CRD"() {
        given:
        server.expect()
                .get()
                .withPath('/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions')
                .andReturn(200, emptyCrdList() ).once()
        server.expect()
                .post()
                .withPath('/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions')
                .andReturn(201, null).once()
        server.expect()
                .get()
                .withPath('/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions')
                .andReturn(200, singlePipelineCrdList() ).once()
        when:
        controller.apply(new Pipeline().definition)

        then:
            def result = client.customResourceDefinitions().list()
            1 == result.items.size()
    }

    def singlePipelineCrdList() {
        new CustomResourceDefinitionListBuilder()
                .addNewItem()
                .withNewMetadata()
                .withName(new Pipeline().crdID)
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
        controller = new KubernetesResourceController( kubernetes: client )
    }

    def cleanup() {
        server.after()
    }
}
