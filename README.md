# Simplifier Content Repository Plugin

## Introduction

The Content-Repository-Plugin is an extension to [Simplifier](http://simplifier.io), adding the capability 
of creating/deleting/accessing files on the file system.

See Simplifier Community documentation for more information: [https://community.simplifier.io](https://community.simplifier.io/doc/current-release/extend/plugins/list-of-plugins/content-repository/)


## Documentation

See here for a short [Plugin documentation](./project/documentation/ContentRepo Plugin Doku Stand 2015-11-12.pdf).



## Deployment

There are two different types of deployment one with a docker container and the second one runs the plugin locally 
(this would require Simplifier to be accessible locally as well)

### Docker Image

The build creates a base image with the necessary files to add to the Simplifier appserver container.

#### Prerequisites

- docker
- Scala 2.12, sbt
- JDK of the target platform (GraalVM-CE 20.0.2)

 
#### Build and Test

At the commandline, run
```bash
sbt test
```

and then


```bash
sbt dockerBuild
```

#### Configuration

The created image is named after the sbt projects name and version. So the result would be something like "contentrepoplugin:0.0.1-SNAPSHOT"

Settings and commandline arguments may be changed by editing [./build/artifacts/assets](./build/artifacts/assets).

The setup process itself is defined by [./build/artifacts/setup.sh](./build/artifacts/setup.sh).


### Appserver build

The docker image from the previous step has to be available in the registry when an appserver image is built.

<b>NOTE:</b> Please use the current SNAPSHOT version.

In the appserver Dockerfile, refer to it as follows:

```dockerfile
FROM contentrepoplugin:0.0.1-SNAPSHOT as contentrepoplugin
```

Below  ```FROM simplifierag/simplifierbase```, insert the following lines:
```dockerfile
# install CONTENT REPOSITORY plugin
COPY --from=contentrepoplugin /opt/plugin /tmp/contentrepoplugin
RUN /tmp/contentrepoplugin/setup.sh /opt/simplifier
```

 
There is only one additional step to be done before building and testing:


### Additional Step: Adding the Oracle JDBC Driver
- in the source tree: create a lib directory
- then copy the Oracle JDBC driver version 23.3 to this directory
- the sourcetree then looks like this:
```
├── build
│   └── artifacts
│       ├── assets
│       │   ├── contentRepoPlugin.arg
│       │   └── contentRepoPlugin.conf
│       ├── build.sh
│       ├── Dockerfile
│       └── setup.sh
├── build.sbt
├── lib
│   └── ojdbc8.jar
├── LICENSE
├── project
├── README.md
├── src
│   ├── main   ...
│   └── test   ...
└── version.sh

```

 

## Local Deployment

The build runs the process with SBT locally, Simplifier's plugin registration port and the plugin communication port must be accessible locally.


### Prerequisites

- A plugin secret has to be available. It can be created in the Plugins section of Simplifier, 
  please look [here for an instruction](https://community.simplifier.io/doc/current-release/extend/plugins/plugin-secrets/).
- replace the default secret: <b>XXXXXX</b> in the [PluginRegistrationSecret](./src/main/scala/byDeployment/PluginRegistrationSecret.scala)
  class with the actual secret.
- Simplifier must be running and the <b>plugin.registration</b> in the settings must be configured accordingly.


### Preparation

#### Simplifier Configuration Modification 

Copy the file [settings.conf.dist](./src/main/resources/settings.conf.dist) as <b>settings.conf</b> to your installation path and edit the values as needed.
When launching the jar, the config file must be given as a commandline argument.

Then set ```fileSystemRepository``` and ```database``` according to your local setup.


__Please note__: Only MySQL and Oracle are supported!

### Build and Run

At the commandline, run
```bash
sbt compile
```

and then

```bash
sbt run
```