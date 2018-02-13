#!/usr/bin/env groovy

import java.nio.file.*

def directory(String directory=null) {
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

def file(Map kv = [:], arg="") {
    def argv = [ path: pwd(temp: true),
                 extension: '.tmp',
                 kind: 'absolute',
                 deleteOnExit: true]
    if (arg instanceof Map) {
        argv << arg
    } else {
        argv.path = arg
    }

    def path = Paths.get( argv.path )
    def directory = path
    def file = ""
    if ( !path.toFile().isDirectory() ) {
        directory =  path.parent ?: Path.get( pwd( tmp: true ) )
        file = path.fileName.toString()
    }

    def temp = Files.createTempFile(directory, file, argv.extension)
    if (argv.deleteOnExit) {
        temp.toFile().deleteOnExit()
    }

    if ('absolute' == argv.kind) {
        temp = temp.toAbsolutePath()
    } else if ('real' == argv.kind) {
        temp = temp.toRealPath()
    }

    return temp.toString()
}

def call(Map kv = [:], arg) {
    return file(arg)
}
