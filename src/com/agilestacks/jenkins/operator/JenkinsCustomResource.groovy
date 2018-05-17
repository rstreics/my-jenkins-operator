package com.agilestacks.jenkins.operator

import com.agilestacks.jenkins.share.StringReplace
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.client.utils.Serialization

@JsonDeserialize(using = JsonDeserializer.None.class)
trait JenkinsCustomResource implements HasMetadata {
    Map<String, ?> spec = [status: [code: Status.UNDEFINED]]

    abstract String getDefinitionCPRef()
    abstract String getCreateScriptCPRef()
    abstract String getDeleteScriptCPRef()

    @Lazy
    Definition definition = { Definition.fromClasspath( definitionCPRef ) }()

    String getCrdID() {
        return definition.metadata.name
    }

    @JsonProperty("spec")
    def setSpec(def newSpec = [:]) {
        spec << newSpec
    }

    String getCreateScript() {
        String template = JenkinsCustomResource.getResourceAsStream(createScriptCPRef).text
        return new StringReplace().mustache(template, this.asMap())
    }

    String getDeleteScript() {
        String template = JenkinsCustomResource.getResourceAsStream(deleteScriptCPRef).text
        return new StringReplace().mustache(template, this.asMap())
    }

    Status getStatus() {
        String code = this.status?.code
        return code ? Status.valueOf( code.toUpperCase() ) : Status.UNDEFINED
    }

    void setStatus(Status code) {
        spec.status.code = code
    }

    def asMap(Map map=['spec': spec,
                       'metadata.name': metadata?.name,
                       'metadata.namespace': metadata?.namespace]) {
        return map.collectEntries { k, v ->
            v instanceof Map ?
                    asMap(v).collectEntries { k1, v1 ->
                        [ "${k}.${k1}": v1 ]
                    }
                    : [ (k): v ]
        }
    }

    static class Definition {

        private Map model

        @Override
        Object getProperty(String propertyName) {
            return model.get(propertyName)
        }

        @Override
        void setProperty(String propertyName, Object newValue) {
            model.put(propertyName, newValue)
        }

        String toJsonString() {
            def json = Serialization.jsonMapper()
            json.writeValueAsString( model )
        }

        static Definition fromClasspath(String cpRef) {
            def payload = JenkinsCustomResource.getResourceAsStream(cpRef)?.text
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

    static enum Status {
        PENDING,
        CONVERGED,
        ERROR,
        UNDEFINED
    }
}
