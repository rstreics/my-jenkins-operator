#!/usr/bin/env groovy

import java.nio.file.*

def directory(String directory) {
    def argv = [ directory: pwd(),
                 kind: 'absolute' ]
    if (arg instanceof Map) {
        argv << arg
    } else {
        argv.directory = arg
    }

    def dir = Files.createTempFile(argv.path, argv.extension)
    if ('absolute' == argv.kind) {
        return dir.toAbsolutePath().toString()
    }
    return dir.toRealPath().toString()
}

def file(Map kv = [:], arg) {
    def argv = [ path: pwd(temp: true),
                 extension: '.tmp',
                 kind: 'absolute' ]
    if (arg instanceof Map) {
        argv << arg
    } else {
        argv.path = arg
    }

    def path = Paths.get( argv.path )
    def directory
    def file
    if ( path.toFile().isDirectory() ) {
        directory = path.toString()
        file = ""
    } else {
        directory = path.parent.toString() ?: pwd( tmp: true )
        file = path.fileName.toString()
    }

    def temp = Files.createTempFile(Paths.get(directory),  argv.path, argv.extension)
    if ('absolute' == argv.kind) {
        return temp.toAbsolutePath().toString()
    }
    return temp.toRealPath().toString()
}

def call(Map kv = [:], arg) {
    return file(arg)
}
