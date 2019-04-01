package com.agilestacks.jenkins.operator

import com.agilestacks.jenkins.operator.jenkins.JenkinsHttpClient
import com.agilestacks.jenkins.operator.kubernetes.KubernetesResourceController
import com.agilestacks.jenkins.operator.kubernetes.RateLimiter
import com.agilestacks.jenkins.operator.resources.Credentials
import com.agilestacks.jenkins.operator.resources.EnvVars
import com.agilestacks.jenkins.operator.resources.GithubOrganization
import com.agilestacks.jenkins.operator.resources.GithubServer
import com.agilestacks.jenkins.operator.resources.Pipeline
import com.agilestacks.jenkins.operator.resources.PipelineLibrary
import com.agilestacks.jenkins.operator.resources.Slack
import com.sun.net.httpserver.HttpServer
import groovy.util.logging.Slf4j
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.client.DefaultKubernetesClient

@Slf4j
class Main {
    static final DEFAULT_NAMESPACE = 'jenkins'
    static final MAX_RETRY = 60
    static boolean ready = false

    static def fromNamespaceFile(File file = new File('/run/secrets/kubernetes.io/serviceaccount/namespace')) {
        return file.exists() ? file.text.trim() : null
    }

    static def kubernetesClient(url) {
        if (url) {
            return new DefaultKubernetesClient(url as String)
        }
        return null
    }

    static def kubernetesClientFromEnv() {
        def kubernetesHost = System.getenv('KUBERNETES_SERVICE_HOST')
        def kubernetesPort = System.getenv('KUBERNETES_SERVICE_PORT') ?: '443'
        if (kubernetesHost) {
            def protocol = kubernetesPort == '443' ? 'https' : 'http'
            def url = "${protocol}://${kubernetesHost}:${kubernetesPort}"
            return kubernetesClient(url)
        }
        return null
    }

    static def startReadinessProbeEndpoint() {
        HttpServer.create(new InetSocketAddress(8000), 0).with {
            it.createContext("/status", { http ->
                http.responseHeaders.add("Content-type", "application/json; charset=utf-8")
                http.sendResponseHeaders(ready ? 200 : 500, 0)
                http.responseBody.withWriter { out ->
                    out << "{\"ready\":${ready}}"
                }
                http.requestBody.close()
            })
            it.start()
        }
    }

    static void main(String[] args) {
        def customResources = [
            Credentials, EnvVars, GithubOrganization,
            GithubServer, Pipeline, PipelineLibrary, Slack
        ] as Class<CustomResource>[]

        def cli = new CliBuilder(usage: 'ls')
        cli.help('print this message')
        cli.ns(longOpt: 'namespace', valueSeparator: '=', args: 1, argName: 'namespace', 'Namespace for operator to watch')
        cli.kurl(longOpt: 'kubernetesUrl', valueSeparator: '=', args: 1, argName: 'kubernetesUrl', 'Url of Kubernetes API server')
        cli.jurl(longOpt: 'jenkinsUrl', valueSeparator: '=', args: 1, argName: 'jenkinsUrl', 'Url of jenkins master')
        cli.ju(longOpt: 'jenkinsUsername', valueSeparator: '=', args: 1, argName: 'username', 'Jenkins username')
        cli.jp(longOpt: 'jenkinsPassword', valueSeparator: '=', args: 1, argName: 'password', 'Jenkins password')

        def options = cli.parse(args)

        String jenkinsUrl = options.jenkinsUrl \
                            ?: System.getenv('JENKINS_URL')

        if (!jenkinsUrl && System.getenv('JENKINS_SERVICE_HOST')) {
            def host = System.getenv('JENKINS_SERVICE_HOST')
            def port = System.getenv('JENKINS_SERVICE_PORT') ?: '80'
            def protocol = port == '443' ? 'https' : 'http'
            jenkinsUrl = "${protocol}://${host}:${port}"
        }

        if (!jenkinsUrl) {
            jenkinsUrl = 'http://localhost:8080'
        }

        String jenkinsUsername = options.jenkinsUsername ?: System.getenv('JENKINS_USERNAME')
        String jenkinsPassword = options.jenkinsPassword ?: System.getenv('JENKINS_PASSWORD')

        def kubeconfig = Config.autoConfigure(null)
        kubeconfig.setMaxConcurrentRequestsPerHost(customResources.length)

        def kubernetesClient = kubernetesClient(options.kurl) \
                                    ?: kubernetesClientFromEnv() \
                                    ?: new DefaultKubernetesClient(kubeconfig)

        def namespace = options.namespace \
                        ?: System.getenv('NAMESPACE') \
                        ?: fromNamespaceFile() \
                        ?: DEFAULT_NAMESPACE

        if (namespace) {
            kubernetesClient = kubernetesClient.inNamespace(namespace) as DefaultKubernetesClient
        }

        def rateLimiter = new RateLimiter()
        def jenkinsClient = new JenkinsHttpClient(jenkinsUrl, jenkinsUsername, jenkinsPassword)
        def controller = new KubernetesResourceController(
            kubernetes: kubernetesClient,
            queue: rateLimiter,
            jenkins: jenkinsClient
        )

        startReadinessProbeEndpoint()

        for (i in 0..MAX_RETRY) {
            log.info "Connecting to Jenkins: ${jenkinsUrl}"
            try {
                def version = jenkinsClient.ping()
                log.info "Connected to Jenkins v${version}"
                break
            } catch (SocketTimeoutException | ConnectException err) {
                if (i == MAX_RETRY) {
                    throw err
                }
                sleep 3000
                log.info "Retry (${i})"
            }
        }

        log.info "Connecting to Kubernetes: ${kubernetesClient.masterUrl}, namespace: ${kubernetesClient.namespace}"
        kubernetesClient.rootPaths()
        log.info "Connected"

        rateLimiter.startAtFixedRate()

        customResources.each {it ->
            controller.apply(it)
            controller.watch(it)
        }

        ready = true
        log.info 'Operator started'
    }
}
