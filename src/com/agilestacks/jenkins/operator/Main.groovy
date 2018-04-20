package com.agilestacks.jenkins.operator

import io.fabric8.kubernetes.client.DefaultKubernetesClient

class Main {

  static final private String DEFAULT_NAMESPACE = 'jenkins'
  static final def OPTS = [:]

  static def fromNamespaceFile(File file = new File('/run/secrets/kubernetes.io/serviceaccount/namespace')) {
    return file.exists() ? file.text.trim() : null
  }

  static void main(String[] args) {
    def cli = new CliBuilder(usage:'ls')
    cli.help('print this message')
    cli.ns(longOpt: 'namespace', valueSeparator:'=', args:1, argName:'namespace', 'Namespace for operator to watch')
    def options = cli.parse(args)

    String namespace = options.namespace \
                        ?: System.getenv('KUBE_NAMESPACE') \
                        ?: fromNamespaceFile() \
                        ?: DEFAULT_NAMESPACE

    def client = new DefaultKubernetesClient().inNamespace(namespace)
    try {
      def pipelines = new PipelineController(kubernetes: client)

      pipelines.registerCrd()
    } finally {
      client.close()
    }
  }
}
