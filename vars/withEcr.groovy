#!/usr/bin/env groovy

@Grab(group = 'com.amazonaws', module = 'aws-java-sdk', version = '1.11.119')
import com.amazonaws.services.ecr.model.*
import com.amazonaws.services.ecr.*

def call(params = [:], Closure body) {
//    def region = params?.region ?: Regions.currentRegion.name
    def data =  AmazonECRClientBuilder
                    .defaultClient()
                    .getAuthorizationToken(new GetAuthorizationTokenRequest())
                    .authorizationData.first()
    if (!data) {
        error "I was not able to receive ecr auth data"
    }

    def endpoint = data.proxyEndpoint
    def token = data.authorizationToken.decodeBase64()
    def login = new String(token).tokenize(':')

    def returnStatus = sh script: "docker login -u ${login[0]} -p ${login[1]} ${endpoint}",
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
