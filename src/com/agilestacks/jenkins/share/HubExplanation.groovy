package com.agilestacks.jenkins.share

import groovy.json.JsonSlurper

class HubExplanation {

    Map<String, ?> explain

    HubExplanation(String json) {
        explain = new JsonSlurper().parseText(json) as Map
    }

    Map<String, String> getStackOutputs() {
        return explain.stackOutputs as Map
    }

    Map<String, String> getStackParameters() {
        return explain.stackParameters as Map
    }

    String [] getProvides() {
        return (explain.provides as Map).keySet()
    }

    Map<String, String> getComponentOutputs(String component) {
        explain.components[(component)]?.outputs as Map
    }

    Map<String, String> getComponentParameters(String component) {
        explain.components[(component)]?.parameters as Map
    }

    Map<String, String> componentOutputs(String component) {
        getComponentOutputs(component)
    }

    Map<String, String> componentParameters(String component) {
        getComponentParameters(component)
    }
}
