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

@Log
@JsonDeserialize(using = JsonDeserializer.None.class)
trait ScriptableResource implements HasMetadata {

    static final MAGIC_STRING = /(?i)\s*Status\s*:\s+CONVERGED\s*<EOF>\s*/
    static final EXCEPTION = /\.\w*Exception:/

    final Map<String, ?> defaults = [:]
    Map<String, ?> spec = [:]

    abstract String getDefinitionFile()
    abstract String getCreateScriptFile()
    abstract String getDeleteScriptFile()

    private Definition definition = null

    String getCreateScript() {
      return formatGroovyScriptFromClasspath( createScriptFile )
    }

    String getDeleteScript() {
      return formatGroovyScriptFromClasspath( deleteScriptFile )
    }

    Definition getDefinition() {
        if (definition == null) {
            definition = Definition.fromClasspath( definitionFile )
        }
        definition
    }

    String getCrdID() {
        return getDefinition().metadata.name
    }

    @JsonProperty("spec")
    void setSpec(Map<String, ?> newSpec) {
        this.spec.putAll(newSpec)
    }

    def create(JenkinsHttpClient jenkins, KubernetesClient kubernetes=null) {
        def text = getCreateScript()
        sendScript(text, jenkins)
    }

    def delete(JenkinsHttpClient jenkins, KubernetesClient kubernetes=null) {
        def text = getDeleteScript()
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
         spec: getDefaults() + getSpec(),
         metadata: metadata.properties]
    }

    String formatGroovyScriptFromClasspath(String filename, Map params=getMergedWithDefaults()) {
        String text = ScriptableResource.getResourceAsStream(filename).text
        formatGroovyScript(text)
    }

    String formatGroovyScript(String text, Map params=getMergedWithDefaults()) {
        def templater = new StringReplace()
        log.info("Apply script parameters: ${params}")
        def rendered = templater.mustache(text, params)
        templater.eraseMustache(rendered)
    }

    @Log
    static class Definition extends CustomResourceDefinition {

        static Definition fromClasspath(String cpRef) {
            def payload = ScriptableResource.getResourceAsStream(cpRef)?.text
            if (!payload) {
                throw new RuntimeException("Cannot find ${cpRef} in classpath")
            }

            def ext = cpRef.split('\\.').last()
            final mapper
            if (ext == 'yml' || ext == 'yaml' ) {
                mapper = Serialization.yamlMapper()
            } else {
                mapper = Serialization.jsonMapper()
            }
            mapper.readValue(payload, Definition)
//            new Definition(model: mapper.readValue(payload, Map) )
        }

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
