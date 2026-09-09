# teamcity-warm-agents-examples
Configuration examples for the Warm Agents plugin for TeamCity. 

Check out [example requests](example-requests.http) to learn how to interact with the plugin. 

To collect warm agents metrics, see the [Prometheus config](example-prometheus.yml) example.

To see how to scale warm agents automatically from TeamCity builds, take a look at the example [Kotlin DSL settings](.teamcity/settings.kts) -- a simple project with an EC2 cloud profile and a scaling build that runs on a schedule trigger two times per day. 
