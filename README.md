# Simplifier Content Repo Plugin

## Deployment
deployment is basically the same as described in the [Simplifier Pdf Plugin](https://github.com/simplifier-ag/PdfPlugin)

there is only on additions step to be done before building and testing:
### Adding the Oracle JDBC Driver
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

## Manual Installation

Copy the file [settings.conf.dist](./src/main/resources/settings.conf.dist) to your installation path and edit the values as needed.
When launching the jar, the config file must be given as a commandline argument.

Then set ```fileSystemRepository``` and ```database``` according to your local setup.
