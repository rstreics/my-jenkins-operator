package com.agilestacks.jenkins.operator

import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.internal.CustomResourceDefinitionOperationsImpl
import okhttp3.Request
import okhttp3.RequestBody

import java.util.logging.Logger

import static com.agilestacks.jenkins.operator.JenkinsCustomResource.*

class CustomResourceController<T extends JenkinsCustomResource> implements Watcher<T> {
    static final log = Logger.getLogger(CustomResourceController.name)

    KubernetesClient kubernetes
    RateLimiter queue
    JenkinsHttpClient jenkins

    def crdOps() {
        return kubernetes.customResourceDefinitions() as CustomResourceDefinitionOperationsImpl
    }

    def registerCRD(T model) {
        def crdID = model.crdID

        def existing = crdOps().list().items.find{ crdID == it.metadata.name }
        if (existing) {
            log.info("${crdID} already exists skippping cretation...")
            return
        }
        log.info("Creating custom resource definition: ${crdID}")

        def payload = model.definition.toJsonString()

        def crdOps = crdOps()
        def request = new Request.Builder()
                .post(RequestBody.create(crdOps.JSON, payload))
                .url(crdOps.resourceUrl)
                .build()
        def resp = crdOps.client.newCall(request).execute()
        log.finest("Response: [${resp.toString()}]")
        if (resp.code() > 400) {
            throw new RuntimeException("Unable to create CRD [resp: ${resp.code()}, text: ${resp.body().string()}]")
        }
    }

    @Override
    void eventReceived(Action action, T resource) {
        if (resource.status == Status.CONVERGED) {
            log.fine("${resource.metadata.name} has been already convered. Doing nothing...")
            return
        }
        if (action == Action.ADDED) {
            queue.enqueue {
                jenkins.postScript(resource.createScript)
                resource.status = Status.CONVERGED
            }
        } else if (action == Action.DELETED) {
            queue.enqueue {
                jenkins.postScript(resource.deleteScript)
                jenkins.postScript(resource.createScript)
            }
        } else if (action == Action.MODIFIED) {
            queue.enqueue {
                jenkins.postScript(resource.deleteScript)
                jenkins.postScript(resource.createScript)
                resource.status = Status.CONVERGED
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
