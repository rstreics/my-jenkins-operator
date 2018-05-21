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

    static final MAGIC_STRING = /(?i)\s*Status\s*:\s+CONVERGED\s*<EOF>\s*/

    Map<String, ?> spec = [:]

    abstract String getDefinitionFile()
    abstract String getCreateScriptFile()
    abstract String getDeleteScriptFile()

    String getCreateScript() {
      return formatGroovyScript( createScriptFile )
    }

    String getDeleteScript() {
      return formatGroovyScript( deleteScriptFile )
    }

    Definition definition = { Definition.fromClasspath( definitionFile ) }()

    String getCrdID() {
        return definition.metadata.name
    }

    @JsonProperty("spec")
    def setSpec(def newSpec = [:]) {
        spec << newSpec
    }

    def asMap(Map map=['spec': spec,
                       'metadata.name': metadata?.name,
                       'metadata.namespace': metadata?.namespace]) {
        return map.collectEntries { k, v ->
            v instanceof Map ?
                    asMap(v).collectEntries { k1, v1 ->
                        [ ("${k}.${k1}".toString()) : v1 ]
                    }
                    : [ (k): v ]
        }
    }

    def sendCreateScript(JenkinsHttpClient jenkins) {
        sendScript(createScript, jenkins)
    }
    def sendDeleteScript(JenkinsHttpClient jenkins) {
        sendScript(createScript, jenkins)
    }

    def sendScript(String script, JenkinsHttpClient jenkins) {
        def resp = jenkins.post('script', ['script': script])
        if (!(resp =~ MAGIC_STRING)) {
            throw new RuntimeException("""Internal error during processing script
                                          [code: ${resp.code()}, text: ${text}]
                                       """.stripIndent().trim())
        }
        resp
    }

    private String formatGroovyScript(String filename) {
        String template = ScriptableResource.getResourceAsStream(filename).text
        return new StringReplace().mustache(template, this.asMap())
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
