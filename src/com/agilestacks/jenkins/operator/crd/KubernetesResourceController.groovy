package com.agilestacks.jenkins.operator.crd

import com.agilestacks.jenkins.operator.JenkinsHttpClient
import com.agilestacks.jenkins.operator.RateLimiter
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.base.OperationSupport
import io.fabric8.kubernetes.client.dsl.internal.CustomResourceDefinitionOperationsImpl
import okhttp3.Request
import okhttp3.RequestBody

import java.util.logging.Logger

class KubernetesResourceController<T extends ScriptableResource> implements Watcher<T> {
    final log = Logger.getLogger(this.class.name)

    DefaultKubernetesClient kubernetes
    RateLimiter queue
    JenkinsHttpClient jenkins

    def getCrd() {
        return kubernetes.customResourceDefinitions() as CustomResourceDefinitionOperationsImpl
    }


    def apply(ScriptableResource.Definition definition) {
        def name = definition.metadata.name

        def existing = crd.list().items.find{ name == it.metadata.name }
        if (existing) {
            log.info("${name} already exists")
            return
        }
        log.info("Creating custom resource definition: ${name}")
        def request = new Request.Builder()
                .post(RequestBody.create( OperationSupport.JSON, definition.toJsonString() ))
                .url(crd.resourceUrl)
                .build()
        def resp = kubernetes.httpClient.newCall(request).execute()

        log.finest("Response: [${resp.toString()}]")
        if (resp.code() > 400) {
            throw new RuntimeException("Unable to create CRD [resp: ${resp.code()}, text: ${resp.body().string()}]")
        }
    }

    def watch(ScriptableResource resource) {
        String name = resource.definition.metadata.name

        def existing = crd.list().items
        if (!existing) {
            throw new RuntimeException("Cannot push CRD ${name}")
        }

        def crd = kubernetes.customResourceDefinitions().withName(name).get()
        kubernetes.
            customResources(crd, resource.class, ScriptableResource.List, ScriptableResource.Done).
            watch(this)
        log.info("Watching ${name}")
    }

    @Override
    void eventReceived(Action action, T resource) {
        log.info "Event received: ${action}, kind: ${resource.kind}, name: ${resource.metadata.name}, apiVersion: ${resource.apiVersion}, "
        if (resource.status == Status.Code.CONVERGED) {
            log.fine("${resource.metadata.name} has been already convered. Doing nothing...")
            return
        }
        if (action == Action.ADDED) {
            queue.enqueue {
                resource.create(jenkins)
                resource.status = Status.Code.CONVERGED
            }
        } else if (action == Action.DELETED) {
            queue.enqueue {
                resource.delete(jenkins)
                resource.status = Status.Code.CONVERGED
            }
        } else if (action == Action.MODIFIED) {
            queue.enqueue {
                resource.delete(jenkins)
                resource.create(jenkins)
                resource.status = Status.Code.CONVERGED
            }
        } else {
            throw new UnsupportedOperationException("Operation ${action} not yet supported")
        }
    }

    @Override
    void onClose(KubernetesClientException cause) {
        queue.shutdown()
    }
}
