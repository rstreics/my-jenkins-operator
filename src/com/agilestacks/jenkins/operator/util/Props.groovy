package com.agilestacks.jenkins.operator.util

import io.fabric8.kubernetes.client.utils.Serialization

trait Props {
    private Map model = [:]

    Map<String, ?> getModel() {
        model
    }

    def setModel(Map<String, ?> newModel) {
        model << newModel
    }

    Object getProperty(String propertyName) {
        return model.get(propertyName)
    }

    void setProperty(String propertyName, Object newValue) {
        model.put(propertyName, newValue)
    }

    String toJsonString() {
        def json = Serialization.jsonMapper()
        json.writeValueAsString( model )
    }
}
