#!/usr/bin/env groovy

import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider

def call(String arn = null) {
    def creds = new STSAssumeRoleSessionCredentialsProvider.Builder(arn, "jenkins-deployer")
            .build()
            .getCredentials()

    return [
            accessKey   : creds.getAWSAccessKeyId(),
            secretKey   : creds.getAWSSecretKey(),
            sessionToken: creds.getSessionToken()
    ]

}
