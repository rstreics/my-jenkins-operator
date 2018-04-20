package com.agilestacks.jenkins.operator

import com.fasterxml.jackson.databind.ObjectMapper
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.internal.CustomResourceDefinitionOperationsImpl
import io.fabric8.kubernetes.client.utils.Serialization
import okhttp3.Request
import okhttp3.RequestBody

import java.util.logging.Logger

class PipelineController {
    static Logger log = Logger.getLogger(PipelineController.name)

    final def CRD = 'pipelines.jenkins.agilestacks.com'

    KubernetesClient kubernetes

    def crdOps() {
        return kubernetes.customResourceDefinitions() as CustomResourceDefinitionOperationsImpl
    }

    def yamlToJson(String yaml) {
        ObjectMapper yamlReader = Serialization.yamlMapper()
        Object obj = yamlReader.readValue(yaml, Object)

        ObjectMapper jsonWriter = Serialization.jsonMapper()
        return jsonWriter.writeValueAsString(obj)
    }

    def createOrUpdateFromClassPath(String filename) {
        log.info("Register custom resource definition: ${CRD}")

        def ext = filename.split('\\.').last()
        def payload = PipelineController.class.getResourceAsStream(filename).text
        if (ext == 'yml' || ext == 'yaml' ) {
            payload = yamlToJson(payload)
        }

        def crdOps = crdOps()
        def request = new Request.Builder()
                                .post(RequestBody.create(crdOps.JSON, payload))
                                .url(crdOps.resourceUrl)
                                .build()
        def resp = crdOps.client.newCall(request).execute()
        log.finest("Response: [${resp.toString()}]")
        def code = resp.code()
        if (code < 200 || code > 300) {
            throw new RuntimeException("Unable to create CRD [resp: ${resp.code()}, ${resp.body().string()}] ")
        }
    }

    def registerCrd() {
        def existing = crdOps().list().items.collect{ it.metadata.name }
        if (CRD in existing) {
            log.info("${CRD} already exists skippping...")
            return
        }
        createOrUpdateFromClassPath('/pipeline-crd.yaml')
    }


}
