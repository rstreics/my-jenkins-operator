#!/usr/bin/env groovy

import com.amazonaws.services.ecr.model.*
import com.amazonaws.services.ecr.*
import com.amazonaws.regions.Regions

def login(region=Regions.currentRegion.name) {
    authenticate(region)
}

boolean interactive() {
    sh(script: '[[ $- == *i* ]]', returnStatus:true) == 0
}

def authenticate(region=Regions.currentRegion.name) {
    def data =  AmazonECRClientBuilder
            .standard()
            .withRegion(region)
            .build()
            .getAuthorizationToken(new GetAuthorizationTokenRequest())
            .authorizationData.first()
    if (!data) {
        error "I was not able to receive ecr auth data"
    }
    def server = data.proxyEndpoint
    def token = data.authorizationToken.decodeBase64()
    def login = new String(token).tokenize(':')

    if (interactive()) {
        def tempDir = pwd(tmp: true)
        def uuid = UUID.randomUUID().toString().replace("-", "").take(5)
        def filename = "${tempDir}/ecr${uuid}.tmp"

        dir( tempDir ) {
            try {
                writeFile(text: login.last(), file: filename)
                def returnStatus = sh(
                        script: "cat ${filename} | docker login --username ${login.first()} --password-stdin ${server}",
                        returnStatus: true)
                if (returnStatus != 0) {
                    error "I was not able to login to ECR {auth.proxyEndpoint}"
                }
            } finally {
                new File(filename).delete()
            }
        }
    } else {
        echo 'Running docker login for non-interractive shell'
        sh "docker login --username ${login.first()} --password ${login.last()} ${server}"
    }
}

def call(region=Regions.currentRegion.name, Closure body=null) {
    authenticate(region)
    if (body != null) {
      try {
        def config = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()
      } catch(Exception e) {
        error e
      }
    }
}
