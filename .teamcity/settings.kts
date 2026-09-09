import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.amazonEC2CloudImage
import jetbrains.buildServer.configs.kotlin.amazonEC2CloudProfile
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.projectFeatures.awsConnection
import jetbrains.buildServer.configs.kotlin.triggers.schedule

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2026.1"

project {

    buildType(ScaleWarmAgents)

    features {
        amazonEC2CloudImage {
            id = "PROJECT_EXT_1"
            profileId = "amazon-1"
            agentPoolId = "-2"
            name = "warm-agents-image"
            vpcSubnetId = "subnet-EXAMPLE1234567890"
            keyPairName = "warm-agents"
            instanceType = "t3.nano"
            securityGroups = listOf("sg-EXAMPLE1234567890")
            instanceTags = mapOf(
                "project" to "warm-agents-demo"
            )
            source = Source("ami-EXAMPLE1234567890")
        }
        awsConnection {
            id = "WarmAgentsDemoProject_AmazonWebServicesAws"
            name = "Amazon Web Services (AWS)"
            regionName = "eu-west-1"
            credentialsType = static {
                accessKeyId = "example-access-key"
                secretAccessKey = "credentialsJSON:00000000-0000-0000-0000-000000000000"
            }
            allowInBuilds = false
            stsEndpoint = "https://sts.eu-west-1.amazonaws.com"
        }
        amazonEC2CloudProfile {
            id = "amazon-1"
            name = "Warm Agents"
            terminateIdleMinutes = 10
            region = AmazonEC2CloudProfile.Regions.EU_WEST_DUBLIN
            awsConnectionId = "WarmAgentsDemoProject_AmazonWebServicesAws"
            maxInstancesCount = 2
        }
    }
}

object ScaleWarmAgents : BuildType({
    name = "Scale Warm Agents"

    params {
        param("warmAgents.target", "0")
        param("warmAgents.projectId", DslContext.projectId.toString())
        param("warmAgents.profileId", "amazon-1")
        param("warmAgents.imageName", "warm-agents-image")
        password("warmAgents.token", "credentialsJSON:11111111-1111-1111-1111-111111111111")
    }

    steps {
        script {
            scriptContent = """
                #!/bin/bash
                
                # check existing WA configurations
                curl --oauth2-bearer %warmAgents.token% -XGET ${DslContext.serverUrl}/app/%warmAgents.projectId%/warmAgents 
                
                # update the target to the parameter value
                curl --oauth2-bearer %warmAgents.token% -XPUT ${DslContext.serverUrl}/app/%warmAgents.projectId%/%warmAgents.profileId%/%warmAgents.imageName%/warmAgents?target=%warmAgents.target%
            """.trimIndent()
        }
    }

    triggers {
        // schedule trigger to scale warm agents up to 2 each morning
        schedule {
            branchFilter = ""
            triggerBuild = always()
            withPendingChangesOnly = false

            schedulingPolicy = daily {
                hour = 8
            }

            buildParams {
                param("warmAgents.target", "2")
            }
        }

        // schedule trigger to scale warm agents down to 0 by night
        schedule {
            branchFilter = ""
            triggerBuild = always()
            withPendingChangesOnly = false

            schedulingPolicy = daily {
                hour = 22
            }

            buildParams {
                param("warmAgents.target", "0")
            }
        }
    }
})
