package com.agilestacks.jenkins.operator.util

import com.agilestacks.jenkins.operator.util.ScriptableResource
import io.fabric8.kubernetes.client.utils.Serialization

trait BasicScriptableRoutines<T extends ScriptableResource> {
    static final MAGIC_STRING = /(?i)\s*print(ln)?\s*[(]?\s*['"]Status:\s+CONVERGED\s*<EOF>\s*['"]\s*[)]?\s*[;]?\s*/

    boolean hasDefinitionAndScripts(T rsc) {
        rsc.definition && rsc.createScript && rsc.deleteScript
    }

    T fromYaml(String filepath, Class<T> clazz) {
        final mapper = Serialization.yamlMapper()
        def text = this.class.getResourceAsStream(filepath).text
        mapper.readValue(text, clazz)
    }
}
