#!/usr/bin/env groovy

import java.nio.file.Files

def call(String name, Closure body=null) {
    return call(prefix: name, body)
}

def call(def arg=[:], Closure body=null) {
    def argv = [ prefix: '',
                 directory: pwd(tmp: true),
                 kind: 'absolute',
                 deleteOnExit: true, ]

    if (arg instanceof Map) {
        argv << arg.findAll{it.value != null}
    } else {
        argv.prefix = arg?.toString()
    }

    def parent = new File( argv.directory as String).toPath()
    if ( Files.notExists(parent) ){
        parent.toFile().mkdirs()
    }

    def dir = Files.createTempDirectory(parent, argv.prefix as String)
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
            } catch (all) {
                error all?.message ?: all.toString()
            }

            if (argv.deleteOnExit) {
                deleteDir()
            }
        }
    }

    return result
}
