package com.agilestacks.jenkins.operator.crd

import com.agilestacks.jenkins.operator.JenkinsHttpClient
import com.agilestacks.jenkins.operator.util.Props
import com.agilestacks.jenkins.share.StringReplace
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.fabric8.kubernetes.api.model.Doneable
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.KubernetesResourceList
import io.fabric8.kubernetes.api.model.ListMeta
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition
import io.fabric8.kubernetes.client.utils.Serialization
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@JsonDeserialize(using = JsonDeserializer.None.class)
trait ScriptableResource implements HasMetadata, Status {

    final log = java.util.logging.Logger.getLogger(this.class.name)

    static final MAGIC_STRING = /(?i)\s*Status\s*:\s+CONVERGED\s*<EOF>\s*/
    static final EXCEPTION = /\.\w*Exception:/

    Map<String, ?> spec = [:]

    abstract String getDefinitionFile()
    abstract String getCreateScriptFile()
    abstract String getDeleteScriptFile()

    String getCreateScript() {
      return formatGroovyScriptFromClasspath( createScriptFile )
    }

    String getDeleteScript() {
      return formatGroovyScriptFromClasspath( deleteScriptFile )
    }

    Definition definition = { Definition.fromClasspath( definitionFile ) }()

    String getCrdID() {
        return definition.metadata.name
    }

    @JsonProperty("spec")
    void setSpec(Map newSpec) {
        this.spec << newSpec
    }

    def create(JenkinsHttpClient jenkins) {
        sendScript(createScript, jenkins)
    }

    def delete(JenkinsHttpClient jenkins) {
        sendScript(createScript, jenkins)
    }

    def sendScript(String script, JenkinsHttpClient jenkins) {
        def resp = jenkins.post('scriptText', ['script': script])
        if (resp =~ EXCEPTION) {
            log.severe("Error from ${jenkins.masterUrl.toString()}:\n${resp}")
        } else {
            log.info("Script output form ${jenkins.masterUrl.toString()}:\n${resp}")
        }
        if (!(resp =~ MAGIC_STRING)) {
            throw new RuntimeException("Internal error during processing script:\n${resp}".stripIndent().trim())
        }
        resp
    }

    Map toMap() {
        ['kind': kind,
         'apiVersion': apiVersion,
         'spec': spec,
         'metadata': metadata.properties]
    }

    String formatGroovyScriptFromClasspath(String filename) {
        String text = ScriptableResource.getResourceAsStream(filename).text
        formatGroovyScript(text)
    }

    String formatGroovyScript(String text) {
        def templater = new StringReplace()
        def rendered = templater.mustache(text, this.toMap())
        templater.eraseMustache(rendered)
    }

    static class Definition extends CustomResourceDefinition implements Props {

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
            new Definition(model: mapper.readValue(payload, Map) )
        }
    }

    static class List<T extends ScriptableResource> implements KubernetesResourceList<T>, Props {
        ListMeta getMetadata() {
            model.metadata
        }

        java.util.List<T> getItems() {
            model.items
        }
    }

    static class Done implements Doneable {
        private static final Logger logger = LoggerFactory.getLogger(Done)

        Object done() {
            logger.error('Method done() is not implemented yet')
            return null
        }
    }
}
