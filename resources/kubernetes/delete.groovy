package kubernetes

import jenkins.model.Jenkins
import org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud

def NAME = '{{meta.name}}'
def jenk = Jenkins.get()
def cloud = jenk.clouds.find { it.name == NAME }
if (!cloud) {
    println "ERROR: cloud with the name ${NAME} does not exists in Jenkins"
    return
}

if (!cloud in KubernetesCloud) {
    println "ERROR: cloud with the name ${NAME} exists but it is not KubernetesCluster"
    return
}

if (!jenk.clouds.remove(cloud)) {
    println "ERROR: cloud: ${NAME} has not been removed"
    return
}
jenk.save()

println 'Status: CONVERGED <EOF>'
