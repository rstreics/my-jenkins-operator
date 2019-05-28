#!/usr/bin/env groovy


import java.util.logging.Logger
import io.jenkins.plugins.artifact_manager_jclouds.s3.S3BlobStore;
import io.jenkins.plugins.artifact_manager_jclouds.s3.S3BlobStoreConfig;
import io.jenkins.plugins.artifact_manager_jclouds.JCloudsArtifactManagerFactory;
import jenkins.model.ArtifactManagerConfiguration;
import io.fabric8.kubernetes.api.model.ConfigMap
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.client.*

def log = Logger.getLogger(this.class.name)

def client = new DefaultKubernetesClient(new ConfigBuilder().build())
if (!client.masterUrl || !client.namespace) {
    println 'Cannot detect Kubernetes nature'
    return
}

def configmap = client.
    configMaps().
    withLabels([
        project: 'jenkins',
        qualifier: 'storage-config'
    ]).list().items[0]?.data


def CONTAINER_NAME =  configmap?.BUCKET_NAME ?: ''
def CONTAINER_PREFIX = "jenkins_stash/"
def BUCKET_KIND = configmap?.BUCKET_KIND ?: ''

if(BUCKET_KIND.trim() == ("s3") && !CONTAINER_NAME.trim().isEmpty() && !CONTAINER_PREFIX.trim().isEmpty() ){
    log.info 'Configuring S3 storage for artifact storage'
    S3BlobStore provider = new S3BlobStore();
    S3BlobStoreConfig config = S3BlobStoreConfig.get();
    config.setContainer(CONTAINER_NAME);
    config.setPrefix(CONTAINER_PREFIX+"/");
    config.doValidateS3BucketConfig(CONTAINER_NAME,CONTAINER_PREFIX);

    JCloudsArtifactManagerFactory artifactManagerFactory = new JCloudsArtifactManagerFactory(provider);
    ArtifactManagerConfiguration.get().getArtifactManagerFactories().add(artifactManagerFactory);

    log.info '"S3 storage" has been configured successfully'
}else{
    log.warning("S3 storage not configured, due parameters being empty ")
}


