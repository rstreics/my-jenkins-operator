#!/usr/bin/env groovy

import java.nio.file.*

def call(String path) {
    return call(path: path)
}

def call(Map arg=[:], Closure body=null) {
    def argv = [ directory: pwd(tmp: true),
                 kind: 'absolute',
                 deleteOnExit: false ] << arg.findAll{it.value != null}

    def dir = Files.createTempFile(argv.path, argv.extension)
    def result
    if ('absolute' == argv.kind) {
        result =  dir.toAbsolutePath().toString()
    } else if ('real' == argv.kind) {
        result = dir.toRealPath().toString()
    } else {
        result = dir.toString()
    }

    if (body != null) {
        dir(result) {
            try {
                def config = [:]
                body.resolveStrategy = Closure.DELEGATE_FIRST
                body.delegate = config
                body()
            } catch (Exception e) {
                error e
            }

            if (argv.deleteOnExit) {
                deleteDir()
            }
        }
    }

    return result
}
