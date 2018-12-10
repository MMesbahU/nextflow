/*
 * Copyright 2018, WuxiNextcode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nextflow.cloud.gcp.pipelines

import com.google.cloud.storage.contrib.nio.CloudStorageFileSystem
import nextflow.Session
import nextflow.exception.AbortOperationException
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path

class GooglePipelinesExecutorTest extends Specification {

    @Shared
    def validZoneConfig = [
            "gcp" : [
            "project" : "testProject",
            "zone" : "testZone1,testZone2"
            ]
    ]

    @Shared
    def validRegionConfig = [
            "gcp" : [
                    "project" : "testProject",
                    "region" : "testRegion1,testRegion2"
            ]
    ]

    def 'should abort operation when the workdir is not a CloudStoragePath'() {
        given:
        def session = Stub(Session)
        session.workDir = Stub(Path)
        def executor = new GooglePipelinesExecutor()
        executor.session = session

        when:
        executor.register()

        then:
        def error = thrown(AbortOperationException)
        error.getMessage().contains("GCE bucket must be provided as a working directory")
    }

    def 'should abort operation when project is not specified'() {
        given:
        def session = Stub(Session)
        def path = CloudStorageFileSystem.forBucket("test").getPath("/")
        session.workDir >> path
        session.bucketDir >> null
        session.config >> [
                "gcp" : [
                        "zone" : "testZone"
                ]
        ]
        def executor = new GooglePipelinesExecutor()
        executor.session = session

        when:
        executor.register()

        then:
        def error = thrown(AbortOperationException)
        error.getMessage() == "Required config value 'gcp.project' for executor null is not defined -- Please add it to your process or nextflow configuration file"
    }

    def 'should abort operation when neither zone or region are specified'() {
        given:
        def session = Stub(Session)
        def path = CloudStorageFileSystem.forBucket("test").getPath("/")
        session.workDir >> path
        session.bucketDir >> null
        session.config >> [
                "gcp" : [
                        "project" : "testproject"
                ]
        ]
        def executor = new GooglePipelinesExecutor()
        executor.session = session

        when:
        executor.register()

        then:
        def error = thrown(AbortOperationException)
        error.getMessage().contains("Missing configuration value 'gcp.zone' or 'gcp.region'")
    }


    def 'should abort operation when both zone and region are specified'() {
        given:
        def session = Stub(Session)
        def path = CloudStorageFileSystem.forBucket("test").getPath("/")
        session.bucketDir >> path
        session.config >> [
                "gcp" : [
                        "project" : "testproject",
                        "zone" : "testZone",
                        "region" : "testRegion"
                ]
        ]
        def executor = new GooglePipelinesExecutor()
        executor.session = session

        when:
        executor.register()

        then:
        def error = thrown(AbortOperationException)
        error.getMessage().contains("You can't specify both 'gcp.zone' and 'gcp.region' configuration parameters -- Please remove one of them from your configuration")
    }



    @Ignore
    def 'should abort operation when required configuration keys are missing'() {
        given:
        def session = Stub(Session)
        def path = CloudStorageFileSystem.forBucket("test").getPath("/")
        session.workDir >> path
        session.config >> [
                "gcp" : [
                        (key) : configValue
                ]
        ]
        def executor = new GooglePipelinesExecutor()
        executor.session = session

        when:
        executor.register()

        then:
        def error = thrown(AbortOperationException)
        !error.getMessage().contains(configKey.toString())

        where:
        key         |   configKey     |   configValue
        "project"   | "gcp.project"   |   "testProject"
        "zone"      | "gcp.zone"      |   "testZone"
    }

    def 'should register successfully with zone'()  {
        given:
        def session = Stub(Session)
        def helper = Mock(GooglePipelinesHelper)
        def path = CloudStorageFileSystem.forBucket("test").getPath("/")
        session.bucketDir >> path
        session.config >> validZoneConfig
        def executor = new GooglePipelinesExecutor(helper: helper)
        executor.session = session

        when:
        executor.register()

        then:
        executor.pipelineConfig.project == validZoneConfig.gcp?.project
        executor.pipelineConfig.zone == validZoneConfig.gcp?.zone?.split(",")?.toList()
    }

    def 'should register successfully with region'()  {
        given:
        def session = Stub(Session)
        def helper = Mock(GooglePipelinesHelper)
        def path = CloudStorageFileSystem.forBucket("test").getPath("/")
        session.bucketDir >> path
        session.config >> validRegionConfig
        def executor = new GooglePipelinesExecutor(helper: helper)
        executor.session = session

        when:
        executor.register()

        then:
        executor.pipelineConfig.project == validRegionConfig.gcp?.project
        executor.pipelineConfig.region == validRegionConfig.gcp?.region?.split(",")?.toList()
    }

    def 'should be containerNative'() {
        when:
        def executor = new GooglePipelinesExecutor()

        then:
        executor.isContainerNative()
    }
}
