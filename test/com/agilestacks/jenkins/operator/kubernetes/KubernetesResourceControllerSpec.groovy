package com.agilestacks.jenkins.operator.kubernetes

import com.agilestacks.jenkins.operator.resources.Credentials
import com.agilestacks.jenkins.operator.resources.EnvVars
import com.agilestacks.jenkins.operator.resources.Pipeline
import com.agilestacks.jenkins.operator.util.ScriptableResource
import groovy.util.logging.Log
import io.fabric8.kubernetes.api.model.WatchEvent
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionListBuilder
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.server.mock.KubernetesServer
import spock.lang.Specification

import static io.fabric8.kubernetes.client.Watcher.Action.ADDED
import static io.fabric8.kubernetes.client.Watcher.Action.DELETED

@Log
class KubernetesResourceControllerSpec extends Specification {
    KubernetesServer kubernetes
    KubernetesResourceController controller

    def "test dummy should be able a valid resource"() {
        given:
        def testDummy = new Dummy()

        expect:
        null != testDummy.createScript
        null != testDummy.createScript
    }

    def "should be able to apply a definitions"() {
        given:
        kubernetes
            .expect()
            .get()
            .withPath('/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions')
            .andReturn(200, getEmptyDefinitionList()).once()
        kubernetes
            .expect()
            .get()
            .withPath('/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions')
            .andReturn(200, getOneDefinitionList()).once()
        kubernetes
            .expect()
            .get()
            .withPath('/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions')
            .andReturn(200, getOneDefinitionList()).once()

        when:
        def emptyList = kubernetes.client.customResourceDefinitions().list()
        controller.apply(Dummy)
        def oneItemList = kubernetes.client.customResourceDefinitions().list()

        then:
        0 == emptyList.items.size()
        1 == oneItemList.items.size()
    }

    def "should be able to watch a custom resource"() {
        given:
        def resource = new Dummy()
        resource.metadata.name = 'testDummy'
        resource.metadata.namespace = 'default'

        kubernetes
            .expect()
            .withPath('/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions')
            .andReturn(200, definitions)
            .always()
        kubernetes
            .expect()
            .withPath('/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions/testdummy.jenkins.agilestacks.com')
            .andReturn(200, resource.definition)
            .always()
        kubernetes
            .expect()
            .withPath('/v1/namespaces/default/dummies')
            .andReturn(200, emptyCustomresourceList)
            .always()
        kubernetes
            .expect()
            .withPath('/v1/namespaces/default/dummies')
            .andReturn(200, emptyCustomresourceList)
            .always()

        kubernetes
            .expect()
            .withPath('/apis/jenkins.agilestacks.com/v1/namespaces/default/dummies')
            .andReturn(200, null)
            .always()

        kubernetes
            .expect()
            .withPath('/apis/jenkins.agilestacks.com/v1/dummies?watch=true')
            .andReturn(200, [new WatchEvent(resource, ADDED.toString())])
            .once()
        kubernetes
            .expect()
            .withPath('/apis/jenkins.agilestacks.com/v1/dummies?watch=true')
            .andReturn(200, [new WatchEvent(resource, DELETED.toString())])
            .once()
        when:
        controller.apply(resource.definition)
        controller.watch(resource)

        controller.getCustomresourceClient(resource).createOrReplace(resource)

        then:
        definitions != null
//            notThrown(Exception)
    }

    def getDefinitions() {
        new CustomResourceDefinitionListBuilder()
            .withItems([new Dummy().definition,
                        new Pipeline().definition,
                        new EnvVars().definition,
                        new Credentials().definition])
            .build()
    }

    def getOneDefinitionList() {
        new CustomResourceDefinitionListBuilder()
            .withItems([new Dummy().definition])
            .build()
    }

    def getEmptyDefinitionList() {
        new CustomResourceDefinitionListBuilder().build()
    }

    def getEmptyCustomresourceList() {
        def list = new ScriptableResource.List<Dummy>()
        list
    }

    def setup() {
        kubernetes = new KubernetesServer(https: true, curdMode: false)
        kubernetes.before()
        def client = kubernetes.client as DefaultKubernetesClient
        def queue = new RateLimiter()
        controller = new KubernetesResourceController(kubernetes: client, queue: queue)

        assert client != null
    }

    def cleanup() {
        kubernetes.after()
    }

    @Log
    static class Dummy extends CustomResource implements ScriptableResource {
        final Definition definition = fromClassPath('/scriptableresource/definition.yaml') as Definition
        final String createScript = fromClassPath('/scriptableresource/create.groovy')
        final String deleteScript = fromClassPath('/scriptableresource/delete.groovy')
    }
}
