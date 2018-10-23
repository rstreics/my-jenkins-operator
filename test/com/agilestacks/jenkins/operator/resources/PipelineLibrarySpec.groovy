package com.agilestacks.jenkins.operator.resources

import com.agilestacks.jenkins.operator.util.BasicScriptableRoutines
import spock.lang.Specification

class PipelineLibrarySpec extends Specification implements BasicScriptableRoutines<Credentials> {

    def "git pipeline library should contain a magic string"() {
        given:
        def resource = fromYaml('/pipelinelibrary1.yaml', PipelineLibrary)

        expect:
        resource.spec.retrieval.containsKey('git')
        resource.createScriptFilename == '/pipelinelibrary/create-git.groovy'
        resource.createScript =~ MAGIC_STRING
    }

    def "file system pipeline library should contain a magic string"() {
        given:
        def resource = fromYaml('/pipelinelibrary2.yaml', PipelineLibrary)

        expect:
        resource.spec.retrieval.containsKey('fileSystem')
        resource.createScriptFilename == '/pipelinelibrary/create-fs.groovy'
        resource.createScript =~ MAGIC_STRING
    }


    def "delete script should contain a magic string"() {
        given:
        def resource = fromYaml('/pipelinelibrary1.yaml', PipelineLibrary)

        expect:
        resource.deleteScript =~ MAGIC_STRING
    }
}
