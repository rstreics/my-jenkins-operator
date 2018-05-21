package com.agilestacks.jenkins.operator

import com.agilestacks.jenkins.operator.crd.KubernetesResourceController
import com.agilestacks.jenkins.operator.crd.ScriptableResource
import io.fabric8.kubernetes.client.DefaultKubernetesClient

import java.util.logging.Logger

class Main {

    static final log = Logger.getLogger(Main.name)

    static final private String DEFAULT_NAMESPACE = 'jenkins'
    static final def OPTS = [:]

    static def fromNamespaceFile(File file = new File('/run/secrets/kubernetes.io/serviceaccount/namespace')) {
        return file.exists() ? file.text.trim() : null
    }

    static void main(String[] args) {
        def cli = new CliBuilder(usage: 'ls')
        cli.help('print this message')
        cli.ns(longOpt: 'namespace', valueSeparator: '=', args: 1, argName: 'namespace', 'Namespace for operator to watch')
        cli.jurl(longOpt: 'jenkinsUrl', valueSeparator: '=', args: 1, argName: 'jenkinsUrl', 'Url of jenkins master')
        cli.ju(longOpt: 'jenkinsUsername', valueSeparator: '=', args: 1, argName: 'username', 'Jenkins username')
        cli.jp(longOpt: 'jenkinsPassword', valueSeparator: '=', args: 1, argName: 'password', 'Jenkins password')

        def options = cli.parse(args)

        String namespace = options.namespace  \
                         ?: System.getenv('KUBE_NAMESPACE')  \
                         ?: fromNamespaceFile()  \
                         ?: DEFAULT_NAMESPACE

        String jenkinsUrl = options.jenkinsUrl  \
                         ?: System.getenv('JENKINS_URL')

        if (!jenkinsUrl && System.getenv('JENKINS_SERVICE_HOST')) {
            def host = System.getenv('JENKINS_SERVICE_HOST')
            def port = System.getenv('JENKINS_SERVICE_PORT') ?: '80'
            def protocol = port == '443' ? 'https' : 'http'
            jenkinsUrl = "${protocol}://${host}:${port}"
        } else if (!jenkinsUrl && !System.getenv('JENKINS_SERVICE_HOST')) {
            jenkinsUrl = 'http://localhost:8080'
        }

        String jenkinsUsername = options.jenkinsUsername
        String jenkinsPassword = options.jenkinsPassword

        def rateLimiter = new RateLimiter()
        def jenkinsClient = new JenkinsHttpClient(jenkinsUrl, jenkinsUsername, jenkinsPassword)
        def kubernetesClient = new DefaultKubernetesClient().inNamespace(namespace)
        try {
            log.info "Connecting to server: ${jenkinsUrl}"
            log.info "Connected to Jenkins v${jenkinsClient.ping()}"

            KubernetesResourceController controller = new KubernetesResourceController(
                kubernetes: kubernetesClient,
                queue: rateLimiter,
                jenkins: jenkinsClient)

            log.info "Connecting to Kubernetes: ${kubernetesClient.masterUrl}"
            def pipe = new Pipeline()
            controller.apply(pipe.definition)
//            controller.watch(pipe)
            rateLimiter.startAtFixedRate()
        } finally {
            log.info "Closing connection to kubernetes"
            kubernetesClient.close()
        }
    }
}
