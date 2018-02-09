#!/usr/bin/env groovy

@Grab(group = 'com.amazonaws', module = 'aws-java-sdk', version = '1.11.119')
import com.amazonaws.services.ecr.model.*
import com.amazonaws.services.ecr.*


def tempFile() {
    def f = File.createTempFile("ecr", ".tmp")
    f.deleteOnExit()
    return f.absolutePath
}

def call(params = [:], arg, Closure body={}) {
    def argv = [ region: Regions.currentRegion.name ]
    if (arg instanceof Map) {
        argv << arg
    } else {
        argv.region = arg
    }

    def data =  AmazonECRClientBuilder
                    .standard()
                    .withRegion(argv.region)
                    .getAuthorizationToken(new GetAuthorizationTokenRequest())
                    .authorizationData.first()
    if (!data) {
        error "I was not able to receive ecr auth data"
    }

    def server = data.proxyEndpoint
    def token = data.authorizationToken.decodeBase64()
    def login = new String(token).tokenize(':')
    def passFile = tempFile()
    writeFile(text: login.last(), file: passFile, )

    def returnStatus = sh script: "cat ${passFile} | docker login --username ${login.first()} --password-stdin ${server}",
                          returnStatus:true
    if (returnStatus != 0) {
      error "I was not able to login to ECR {auth.proxyEndpoint}"
    }

    if (body != null) {
      try {
        body()
      } catch(Exception e) {
        error e
      }
    }
}
