#!/usr/bin/env groovy

@GrabResolver(name = 'central', root = 'http://central.maven.org/maven2/')
@Grab(group = 'com.amazonaws', module = 'aws-java-sdk', version = '1.11.313')
@Grab(group = 'software.amazon.ion', module = 'ion-java', version = '1.0.2')
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
