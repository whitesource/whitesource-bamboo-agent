# White Source agent for Bamboo

A [White Source](http://www.whitesourcesoftware.com/) external update agent for [Atlassian Bamboo](http://www.atlassian.com/software/bamboo/).

## Overview

This plugin allows Bamboo users to easily create new White Source projects and update existing ones via a Task from your build. More features are planned for the near future.
More information can be found on the [Agents](http://docs.whitesourcesoftware.com/display/docs/Agents) project documentation.

### Supported job types

The plugin currently supports Maven and Freestyle jobs. Support for Ivy and Gradle jobs is on the roadmap.

### Status

Actively developed and supported by White Source:

* Please file issues via the White Source project [whitesource-bamboo-agent ](https://github.com/whitesource/whitesource-bamboo-agent/issues)
* You may also contact White Source [Support](http://www.whitesourcesoftware.com/support)

### Release Notes

This is the initial release 1.0.0 - it offers support to create or update your [White Source](http://www.whitesourcesoftware.com/)
Open Source Software (OSS) inventory from a Task within your build, enforcing policies and failing the build if necessary.

#### 1.0.0

Release 1.0.0 adds a Task to create or update a White Source Open Source Software (OSS) inventory, enforcing policies and failing the build if necessary.

## Installation

Installation is performed via the [Universal Plugin Manager](https://marketplace.atlassian.com/plugins/com.atlassian.upm.atlassian-universal-plugin-manager-plugin) as usual, 
see [Installing a plugin](https://confluence.atlassian.com/x/UQU_EQ) for details.

## Configuration

All textual Task parameters support Bamboo variable substitution to allow for global, plan or build-specific settings, 
see [Using global, plan or build-specific variables](https://confluence.atlassian.com/x/nwQ_EQ) for details.

* Please note that if the variable key contains the phrase "password", the value will be masked with "\*\*\*\*\*\*\*\*" in the Build Logs. 
E.g. if the key is password, apiKeyPassword or projectTokenPassword the value will be masked.

### Parameters

#### Maven jobs

* _API key_ - Uniquely identifies your organization's White Source account.
* _Check policies?_ - Checking this option will check policies before any update.
* _Project token_ - Uniquely identifies the project built by the job.
* _Module tokens_ - Map of module artifactId to White Source project token.
* _Module Includes_ - Only modules with an artifactId matching one of these patterns will be processed by the plugin.
* _Module Excludes_ - Modules with an artifactId matching any of these patterns will not be processed by the plugin.
* _Ignore POM modules?_ - Checking this option will ignore maven modules of type POM.

#### Freestyle jobs

* _API key_ - Uniquely identifies your organization's White Source account.
* _Check policies?_ - Checking this option will check policies before any update.
* _Project token_ - Uniquely identifies the project built by the job.
* _File Includes_ - Ant-style [FileSet](http://ant.apache.org/manual/Types/fileset.html) pattern to determine which files to include as dependencies. Relative to the job's workspace.
* _File Excludes_ - Ant-style [FileSet](http://ant.apache.org/manual/Types/fileset.html) pattern to determine which files to exclude as dependencies. Relative to the job's workspace.

### HTTP(S) Proxy

If your Bamboo instance is running behind a firewall, the plugin will reuse the proxy configuration from Bamboo, 
see [Cannot connect to an AWS or EC2 instance when the Bamboo application is running behind a proxy](https://confluence.atlassian.com/x/nAFgDQ) for details.

## License

Licensed under the Apache License, Version 2.0, see LICENSE for details.
