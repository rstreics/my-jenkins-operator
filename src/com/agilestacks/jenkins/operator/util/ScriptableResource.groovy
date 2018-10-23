package com.agilestacks.jenkins.operator.util

import com.agilestacks.jenkins.operator.jenkins.JenkinsHttpClient
import com.agilestacks.jenkins.share.StringReplace
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import groovy.util.logging.Log
import io.fabric8.kubernetes.api.model.Doneable
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.KubernetesResourceList
import io.fabric8.kubernetes.api.model.ListMeta
import io.fabric8.kubernetes.api.model.ListMetaBuilder
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.utils.Serialization

import static com.agilestacks.jenkins.operator.util.Utils.*

@Log
@JsonDeserialize(using = JsonDeserializer.None.class)
trait ScriptableResource implements HasMetadata {

    static final MAGIC_STRING = /(?i)\s*Status\s*:\s+CONVERGED\s*<EOF>\s*/
    static final EXCEPTION = /\.\w*Exception:/

    final Map<String, ?> defaults = [:]
    Map<String, ?> spec = [:]

    abstract Definition getDefinition()
    abstract String getCreateScript();
    abstract String getDeleteScript();

    String getCrdID() {
        return getDefinition().metadata.name
    }

    @JsonProperty("spec")
    void setSpec(Map<String, ?> newSpec) {
        this.spec.putAll(newSpec)
    }

    def create(JenkinsHttpClient jenkins, KubernetesClient kubernetes=null) {
        def text = renderTemplate(createScript, mergedWithDefaults)
        sendScript(text, jenkins)
    }

    def delete(JenkinsHttpClient jenkins, KubernetesClient kubernetes=null) {
        def text = renderTemplate(deleteScript, mergedWithDefaults)
        sendScript(text, jenkins)
    }

    def sendScript(String script, JenkinsHttpClient jenkins) {
        def resp = jenkins.post('scriptText', ['script': script])
        if (resp =~ EXCEPTION) {
            log.severe("Error from ${jenkins.masterUrl.toString()}\n${resp}")
        } else {
            log.info("Script output form ${jenkins.masterUrl.toString()}:\n${resp}")
        }
        if (!(resp =~ MAGIC_STRING)) {
            throw new RuntimeException("Internal error during processing script:\n${resp}".stripIndent().trim())
        }
        resp
    }

    Map<String, ?> getMergedWithDefaults() {
        [kind: kind,
         apiVersion: apiVersion,
         spec: deepMerge(getDefaults(), getSpec()),
         metadata: metadata.properties]
    }

    static def fromClassPath(String filename) {
        def value = ScriptableResource.getResourceAsStream(filename)?.text
        new ClassPathWrapper(value: value)
    }

    static String renderTemplate(String text, Map<String, ?>  params=[:]) {
        def templater = new StringReplace()
        log.info("Apply script parameters: ${params}")
        def rendered = templater.mustache(text, params)
        templater.eraseMustache(rendered)
    }

    static class ClassPathWrapper {
        String value

        Object asType(Class clazz) {
            if (clazz == Definition) {
                def mapper =Serialization.yamlMapper()
                return mapper.readValue(value, Definition)
            }

            return value.asType(clazz)
        }

        String toString() {
            return value
        }
    }

    @Log
    static class Definition extends CustomResourceDefinition {
        String toJsonString() {
            Serialization.jsonMapper().writeValueAsString( this )
        }
    }

    @Log
    static class List<T extends ScriptableResource> implements KubernetesResourceList<T>, Props {
        ListMeta metadata = new ListMetaBuilder().build()
        java.util.List<T> items = []
    }

    @Log
    static class Done implements Doneable {
        Object done() {
            log.error('Method done() is not implemented yet')
            return null
        }
    }
}
