package com.agilestacks.jenkins.operator.kubernetes

import com.agilestacks.jenkins.operator.jenkins.JenkinsHttpClient
import com.agilestacks.jenkins.operator.util.ScriptableResource
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.base.OperationSupport
import io.fabric8.kubernetes.client.dsl.internal.CustomResourceDefinitionOperationsImpl
import okhttp3.Request
import okhttp3.RequestBody

import java.lang.reflect.Constructor
import java.util.logging.Logger

class KubernetesResourceController<T extends ScriptableResource> implements Watcher<T> {
    final log = Logger.getLogger(this.class.name)

    DefaultKubernetesClient kubernetes
    RateLimiter queue
    JenkinsHttpClient jenkins

    def getCrd() {
        return kubernetes.customResourceDefinitions() as CustomResourceDefinitionOperationsImpl
    }

    def apply(Class<T> clazz) {
        Constructor constructor = clazz.constructors.first()
        def rsc = constructor.newInstance() as T
        apply( rsc.definition )
    }

    def apply(ScriptableResource.Definition definition) {
        def name = definition.metadata.name

        def existing = crd.list().items.find{ name == it.metadata.name }
        if (existing) {
            log.info("Found: ${name}")
            return
        }
        log.info("Creating: ${name}")
        def request = new Request.Builder()
                .post(RequestBody.create( OperationSupport.JSON, definition.toJsonString() ))
                .url(crd.resourceUrl)
                .build()
        def resp = kubernetes.httpClient.newCall(request).execute()

        log.finest("Response: [${resp.toString()}]")
        if (resp.code() >= 400) {
            throw new RuntimeException("Unable to create CRD [resp: ${resp.code()}, text: ${resp.body().string()}]")
        }
    }

    def watch(Class<T> clazz, Object[] args=[]) {
        Constructor constructor = clazz.constructors.first()
        def rsc = constructor.newInstance(args) as T
        watch( rsc )
    }

    def getCustomresourceClient( ScriptableResource resource,
                                 CustomResourceDefinition definition=resource.definition ) {
        kubernetes.customResources( definition,
                                    resource.class,
                                    ScriptableResource.List,
                                    ScriptableResource.Done )
    }

    def watch(ScriptableResource resource) {
        String name = resource.definition.metadata.name

        def existing = crd.list().items
        if (!existing) {
            throw new RuntimeException("Cannot push CRD ${name}")
        }

        def crd = kubernetes.customResourceDefinitions().withName(name).get()
        kubernetes.customResources(crd, resource.class, ScriptableResource.List, ScriptableResource.Done)
            .watch(this)
        log.info("Watching ${name}")
    }

    @Override
    void eventReceived(Action action, T resource) {
        log.info "${action}: name: ${resource.metadata.name}, kind: ${resource.kind}, apiVersion: ${resource.apiVersion}"
        if (action == Action.ADDED) {
            queue.enqueue {
                log.info "Proceed with ${resource.metadata.name}@${resource.apiVersion} creation"
                resource.create(jenkins, kubernetes)
                log.info "done"
            }
        } else if (action == Action.DELETED) {
            log.info "Proceed with ${resource.metadata.name}@${resource.apiVersion} deletion"
            queue.enqueue {
                log.info "Proceed with ${resource.metadata.name} deletion"
                resource.delete(jenkins, kubernetes)
                log.info "done"
            }
        } else if (action == Action.MODIFIED) {
            queue.enqueue {
                log.info "Proceed with ${resource.metadata.name}@${resource.apiVersion} update"
                resource.delete(jenkins, kubernetes)
                resource.create(jenkins, kubernetes)
                log.info "done"
            }
        } else {
            log.severe("Unsupported action ${action} for ${resource.metadata.name}")
            throw new UnsupportedOperationException("Operation ${action} not yet supported")
        }
    }

    @Override
    void onClose(KubernetesClientException cause) {
        queue.shutdown()
    }
}
