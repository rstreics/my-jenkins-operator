package com.agilestacks.jenkins.operator

import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

import java.util.logging.Logger

class JenkinsHttpClient  {
    static final MAGIC_STRING = /(?i)\s*Status\s*:\s+CONVERGED\s*<EOF>\s*/

    static final log = Logger.getLogger(JenkinsHttpClient.name)

    HttpUrl masterUrl = HttpUrl.parse('http://localhost:8080')

    JenkinsHttpClient(HttpUrl masterUrl) {
        this.masterUrl = masterUrl
    }

    JenkinsHttpClient(String url, String username = null, String password = null) {
        this( serverUrl(url, username, password) )
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

    static def serverUrl(String url, String username=null, String password=null) {
        def builder = HttpUrl.parse(url).newBuilder()
        if (username) {
            builder.username(username)
        }
        if (password) {
            builder.password(password)
        }
        return builder.build()
    }

    def postScript(String script, HttpUrl baseUrl=masterUrl) {
        def builder = baseUrl
                        .newBuilder()
                        .addPathSegment('script')

        def request = new Request.Builder()
                        .url( builder.build() )
                        .post( new FormBody.Builder().add('script', script).build() )
                        .build()
        log.info("Calling http ${request}")
        def resp = newClient().newCall(request).execute()
        if (resp.code() >= 400) {
            throw new ConnectException("""Unable to ${request.method()} ${request.url().toString()} 
                                          got response [code: ${resp.code()}, text: ${resp.body().string()}]
                                       """.stripIndent().trim())
        }

        def text = resp.body().string()
        if (!(text =~ JenkinsHttpClient.MAGIC_STRING)) {
            throw new RuntimeException("""Internal error during processing script
                                          [code: ${resp.code()}, text: ${text}]
                                       """.stripIndent().trim())
        }

        text
    }

    def ping(HttpUrl baseUrl=masterUrl) {
        def request = new Request.Builder()
                        .url(baseUrl)
                        .get()
                        .build()
        log.finer("Calling http ${request}")
        def resp = newClient().newCall(request).execute()
        if (resp.code() >= 400) {
            throw new ConnectException("""Unable to ${request.method()} ${request.url().toString()} 
                                          got response [code: ${resp.code()}, text: ${resp.body().string()}]
                                       """.stripIndent().trim())
        }
        return resp.header('X-Jenkins') ?:  'Unknown'
    }

}
