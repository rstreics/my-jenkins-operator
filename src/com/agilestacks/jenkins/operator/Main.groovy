package com.agilestacks.jenkins.operator

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
    def cli = new CliBuilder(usage:'ls')
    cli.help('print this message')
    cli.ns(longOpt: 'namespace', valueSeparator:'=', args:1, argName:'namespace', 'Namespace for operator to watch')
    cli.jurl(longOpt: 'jenkinsUrl', valueSeparator:'=', args:1, argName:'jenkinsUrl', 'Url of jenkins master')
    cli.ju(longOpt: 'jenkinsUsername', valueSeparator:'=', args:1, argName:'username', 'Jenkins username')
    cli.jp(longOpt: 'jenkinsPassword', valueSeparator:'=', args:1, argName:'password', 'Jenkins password')

    def options = cli.parse(args)

    String namespace = options.namespace \
                        ?: System.getenv('KUBE_NAMESPACE') \
                        ?: fromNamespaceFile() \
                        ?: DEFAULT_NAMESPACE

    String jenkinsUrl = options.jenkinsUrl \
                        ?: System.getenv('JENKINS_URL')

    if (!jenkinsUrl && System.getenv('JENKINS_SERVICE_HOST')) {
      def host = System.getenv('JENKINS_SERVICE_HOST')
      def port = System.getenv('JENKINS_SERVICE_PORT') ?: '80'
      def protocol = port == '443' ? 'https' : 'http'
      jenkinsUrl =  "${protocol}://${host}:${port}"
    } else if (!jenkinsUrl && !System.getenv('JENKINS_SERVICE_HOST')) {
      jenkinsUrl = 'http://localhost:8080'
    }

    String jenkinsUsername = options.jenkinsUsername
    String jenkinsPassword = options.jenkinsPassword

    def client = new DefaultKubernetesClient().inNamespace(namespace)
    try {
      def limiter = new RateLimiter()
      def jenkinsClient = new JenkinsHttpClient(jenkinsUrl, jenkinsUsername, jenkinsPassword)

      def pipelineController = new CustomResourceController<Pipeline>(
              kubernetes: client,
              queue: limiter,
              jenkins: jenkinsClient)

      log.info "Connecting to Kubernetes: ${client.masterUrl}"
      pipelineController.registerCRD(new Pipeline())

      log.info "Connecting to: ${jenkinsUrl}"
      log.info "Connected to Jenkins v${jenkinsClient.ping()}"
      limiter.startAtFixedRate()
    } finally {
      client.close()
    }
  }
}
