package com.agilestacks.jenkins.share

import groovy.json.JsonSlurper

class HubExplanation {

    Map<String, ?> explain

    HubExplanation(String json) {
        this( new JsonSlurper().parseText(json) as Map )
    }

    HubExplanation(Map<String, ?> state) {
        explain = state
    }

    @Override
    Object getProperty(String propertyName) {
        explain.get(propertyName) ?: super.getProperty(propertyName)
    }
}
