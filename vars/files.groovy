/**
 * Different utilities to work with files
 */

import hudson.FilePath

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Finds directories that corresponds to Ant glob specified via named arg: includes
 *
 * @param args - named args: basedir, includes
 * @return list of string
 */
List<String> findDirs(args=[:]) {
    def argv = [
            basedir: pwd(),
            includes: '**/*',
            absolutePath: true,
    ] << args
    def result = []
    dir(argv.basedir) {
        result = findFiles(glob: argv.includes).
                toList().
                collect {
                    def f = new FilePath(new File(it.path))
                    if (!f.directory) {
                        f = f.parent
                    }
                    f.remote
                }.findAll{it}.unique()
    }
    return result.collect{argv.basedir + File.separator + it}
}

/**
 * Finds directories that corresponds to Ant glob. Searches in current directory (pwd)
 *
 * @param includes - Ant glob to use for search
 * @return list of string
 */
List<String> findDirs(String includes) {
    findDirs(includes: includes)
}

/**
 * Returns temp directory with prefix
 * @param name - prefix
 * @param body - optionally runs a closure
 * @return absolute path as string
 */
def tempDir(String name, Closure body=null) {
    return tempDir(prefix: name, body)
}

/**
 * Returns name of current directory
 * @param args - pwd: true
 * @return string
 */
def pwdDirName(args = [pwd: true]) {
    node('master') {
        String s =  pwd( args )
        def pwd = new FilePath(new File(s))
        return pwd.name
    }
}

/**
 * Returns name of current directory
 * @return string
 */
def getPwdDirName() {
    return pwdDirName()
}

/**
 * Creates a temp directory
 * @param arg named args[prefix, directory (pwd), deleteOnExit]
 * @param body optional closure to execute
 * @return string
 */
def tempDir(def arg=[:], Closure body=null) {
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

/**
 * Create a temp file
 * @param path - path to temp file
 * @param ext - extension of a file
 * @return string as path to the file
 */
def tempFile(path, ext) {
    return tempFile(path: path, extension: ext)
}

/**
 * Creates a temp file
 * @param arg - names args [path, extension, deleteOnExit]
 * @return string as path to the file
 */
def tempFile(Map arg=[:]) {
    String tempDir = pwd(temp: true)
    def argv = [ path: tempDir,
                 extension: '.tmp',
                 kind: 'absolute',
                 deleteOnExit: false ] << arg.findAll{it.value != null}

    def path = Paths.get(argv.path.toString())
    def directory = path
    def filename = ""
    if ( !path.toFile().isDirectory() ) {
        directory =  path.parent ?: Paths.get( tempDir )
        filename = path.fileName.toString()
    }

    if (!Files.exists(directory)) {
        Files.createDirectories(directory)
    }

    def temp = Files.createTempFile(directory, filename, argv.extension.toString())
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