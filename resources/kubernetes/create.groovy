package kubernetes

import jenkins.model.Jenkins
import org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud

def NAME = '{{meta.name}}'
if ( Jenkins.get().clouds.find { it.name == NAME } ) {
    println "Cloud with the name ${NAME} already exists in Jenkins Server. Moving on!"
    return
}

def kube = new KubernetesCloud(NAME)
kube.serverUrl = '{{spec.serverUrl}}'
if (!kube.serverUrl) {
    def k8sHost = System.getenv('KUBERNETES_SERVICE_HOST')
    def k8sPort = System.getenv('KUBERNETES_SERVICE_PORT') ?: '443'
    kube.serverUrl = "${k8sPort == '443' ? 'https' : 'http'}://${k8sHost}:${k8sPort}/"
}

kube.jenkinsUrl = '{{spec.jenkinsUrl}}' ?: System.getenv('JENKINS_URL')
if (!kube.jenkinsUrl) {
    def jenkHost  = System.getenv('JENKINS_SERVICE_HOST')
    def jenkPort  = System.getenv('JENKINS_SERVICE_PORT') ?: '8080'
    kube.jenkinsUrl = "${jenkPort == '443' ? 'https' : 'http'}://${jenkHost}:${jenkPort}/"
}

kube.jenkinsTunnel = '{{spec.jenkinsTunnel}}' ?: "${jenkHost}:50000"
if (!kube.jenkinsTunnel) {
    def url = kube.jenkinsUrl.toURL()
    kube.jenkinsTunnel = jenkins
}

kube.skipTlsVerify = '{{spec.skipTlsVerify}}'.empty ?: true
kube.serverCertificate = '{{spec.serverCertificate}}' ?: null
kube.addMasterProxyEnvVars = '{{spec.addMasterProxyEnvVars}}' ?: false
kube.namespace     = '{{spec.namespace}}' ?: 'default'
kube.credentialsId = '{{spec.credentialsId}}' ?: null
kube.defaultsProviderTemplate = 'default'
kube.containerCap = Integer.MAX_VALUE
kube.retentionTimeout = 5
kube.labels = [jenkins: 'slave']

kube.templates  = [ pod1, pod2 ]
jenk.clouds << kube
jenk.save()

println 'Status: CONVERGED <EOF>'
