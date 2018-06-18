package com.agilestacks.jenkins.operator.kubernetes

import com.agilestacks.jenkins.operator.util.NamespacedResource
import com.agilestacks.jenkins.operator.util.ScriptableResource
import com.agilestacks.jenkins.operator.resources.Credentials
import com.agilestacks.jenkins.operator.resources.EnvVars
import com.agilestacks.jenkins.operator.resources.Pipeline
import groovy.util.logging.Log
import io.fabric8.kubernetes.api.model.WatchEvent
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionListBuilder
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.server.mock.KubernetesServer
import spock.lang.Specification

import static io.fabric8.kubernetes.client.Watcher.Action.*

@Log
class WatcherSpec extends Specification {
    KubernetesServer server
    KubernetesResourceController controller

    def "it is possible to watch even a dummy resource"() {
        given:
            def resource = new Dummy()
            resource.metadata.name = 'testDummy'
            resource.metadata.namespace = 'default'

            server
                .expect()
                .withPath('/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions')
                .andReturn(200, definitions)
                .always()
            server
                .expect()
                .withPath('/apis/apiextensions.k8s.io/v1beta1/customresourcedefinitions/testdummy.jenkins.agilestacks.com')
                .andReturn(200, resource.definition )
                .always()

            server
                .expect()
                .withPath('/v1/namespaces/default/dummies')
                .andReturn(200, emptyCustomresourceList)
                .always()

            server
                .expect()
                .withPath('/apis/jenkins.agilestacks.com/v1/namespaces/default/dummies')
                .andReturn(200, null)
                .always()

            server
                .expect()
                .withPath("${resource.path}?watch=true")
                .andReturn(200, [new WatchEvent(resource, ADDED.toString())])
                .once()
            server
                .expect()
                .withPath("${resource.path}?watch=true")
                .andReturn(200, [new WatchEvent(resource, DELETED.toString())])
                .once()
        when:
            controller.apply(resource.definition)
            controller.watch(resource)

            controller.getCustomresourceClient(resource).createOrReplace( resource )

        then:
            definitions != null
//            notThrown(Exception)
    }

    def getDefinitions() {
        new CustomResourceDefinitionListBuilder()
            .withItems([new Dummy().definition,
                        new Pipeline().definition,
                        new EnvVars().definition,
                        new Credentials().definition ])
            .build()
    }

    def getEmptyCustomresourceList() {
        def list = new ScriptableResource.List<Dummy>()
        list
    }

    def setup() {
        server = new KubernetesServer(https: true, curdMode: false)
        server.before()
        def client = server.client as DefaultKubernetesClient
        def queue = new RateLimiter()
        controller = new KubernetesResourceController(kubernetes: client, queue: queue)

        assert client != null
    }

    def cleanup() {
        server.after()
    }

    @Log
    class Dummy extends CustomResource implements ScriptableResource, NamespacedResource {
        String definitionFile = '/scriptableresource/definition.yaml'
        String createScriptFile = '/scriptableresource/create.groovy'
        String deleteScriptFile = '/scriptableresource/delete.groovy'
    }
}
