flow-platform
============
![](https://img.shields.io/badge/language-java-orange.svg)
[![](http://api-lyon.flow.ci/projects/597aa1c0f2652213cd6aab63/jobs/build_state)](https://lyon.flow.ci)

'flow-platform' is a backend project for flow.ci, it consist three main sub projects:

- api: 
- control-center: to handle agent status and dispatch task to agent
- agent: running in the any where to receive task from control center


## Getting Start
 
### Build

Using standard maven `mvn clean install -DskipTests=true` to generate packages


### Installation

In build phase will generate three packages:

| project | package | container |
|---------|---------|-----------|
| api | flow-api.war | tomcat 8.5 |
| control-center | flow-control-center.war | tomcat 8.5 |
| agent | flow-agent-{version}.jar |  |

### API

- Mysql 5.6
- RabbitMQ 3.6.10

#### Control-Center

##### Dependency

- Apache zookeeper 3.4.10
- Mysql 5.6
- RabbitMQ 3.6.10
##### Configuration

Configuration file can be loaded by sequence:
- System envrionment variable: `FLOW_CONFIG_PATH`
- System property: `-Dconfig.path`
- Default directory: `/etc/flow.ci/config/app.properties`

The sample properites file in : 

`platform-control-center/app-example.properties`

### Agent

Start agent by following command:

`java -jar flow-platform-agent-{version}.jar {zookeeper address} {zone name} $HOSTNAME`


## License
flow-platform is an open source project, sponsored by [fir.im](https://www.fir.im) 
under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
