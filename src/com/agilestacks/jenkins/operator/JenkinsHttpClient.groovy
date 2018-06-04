package com.agilestacks.jenkins.operator

import groovy.util.logging.Log
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

@Log
class JenkinsHttpClient  {

    HttpUrl masterUrl = HttpUrl.parse('http://localhost:8080')

    JenkinsHttpClient(HttpUrl masterUrl) {
        this.masterUrl = masterUrl
    }

    JenkinsHttpClient(String url, String username = null, String password = null) {
        this( toHttpUrl(url, username, password) )
    }

    def newClient(HttpUrl url=masterUrl) {
        def builder = new OkHttpClient.Builder()
        if (url.username()) {
            builder = builder.authenticator(new Authenticator() {
                Request authenticate(Route route, Response response) throws IOException {
                    def credential = Credentials.basic(url.username(), url.password())
                    return response.request().newBuilder().header("Authorization", credential).build()
                }
            })
        }
        return builder.build()
    }

    String post(String uri, Map formBody = [:]) {
        post(uri.toURI(), formBody)
    }

    String post(URI uri, Map formBody = [:]) {
        def url = masterUrl.newBuilder()
        uri.path.split('/').each {
            url.addPathSegment(it)
        }

        def body = new FormBody.Builder()
        formBody.each { k, v ->
            body.add(k, v)
        }

        def request = new Request.Builder()
            .url( url.build() )
            .post( body.build() )
            .build()

        def resp = newClient().newCall(request).execute()
        try {
            if (!resp.successful) {
                throw new ConnectException("""Unable to ${request.method()} ${request.url().toString()}
                                          got response [code: ${resp.code()}, text: ${resp.body().string()}]
                                       """.stripIndent().trim())
            }
            return resp.body().string()
        } finally {
            resp.close()
        }
    }

    def ping() {
        def request = new Request.Builder()
                        .url(masterUrl)
                        .get()
                        .build()
        log.finer("Calling http ${request}")
        def resp = newClient().newCall(request).execute()
        try {
            if (!resp.successful) {
                throw new ConnectException("""Unable to ${request.method()} ${request.url().toString()}
                                          got response [code: ${resp.code()}, text: ${resp.body().string()}]
                                       """.stripIndent().trim())
            }
            return resp.header('X-Jenkins') ?: 'Unknown'
        }finally {
            resp.close()
        }
    }

    private static HttpUrl toHttpUrl(String url, String username=null, String password=null) {
        def builder = HttpUrl.parse(url).newBuilder()
        if (username) {
            builder.username(username)
        }
        if (password) {
            builder.password(password)
        }
        return builder.build()
    }
}
