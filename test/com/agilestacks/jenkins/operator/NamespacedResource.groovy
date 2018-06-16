package com.agilestacks.jenkins.operator

trait NamespacedResource {
    def getPath(args=[:]) {
        "/apis/${this.definition.spec.group}/${this.definition.spec.version}/${this.definition.spec.names.plural}"
    }
}
