# White Source agent for Bamboo

White Source external update agent for Atlassian Bamboo.

## Overview

This plugin allows Bamboo users to easily create new White Source projects and update existing ones via a Task from your build. More features are planned for the near future.
More information can be found on the [Agents](http://docs.whitesourcesoftware.com/display/docs/Agents) project documentation.

### Supported job types

The plugin currently supports Free Style jobs. Support for Maven, Ivy and Gradle jobs is on the roadmap.

### Status

Actively developed (early stages though) and supported by White Source:

* Please file issues via the White Source origin project [whitesource-bamboo-agent ](https://github.com/whitesource/whitesource-bamboo-agent/issues)
* You may also contact White Source [Support](http://www.whitesourcesoftware.com/support)

### Release Notes

NOTE: There is no official release yet!

#### ~~1.0.0~~

~~Release 1.0.0 adds a Task to create or update a White Source Open Source Software (OSS) inventory.~~

## Installation

Installation is performed via the [Universal Plugin Manager](https://marketplace.atlassian.com/plugins/com.atlassian.upm.atlassian-universal-plugin-manager-plugin) as usual, 
see [Installing a plugin](https://confluence.atlassian.com/x/UQU_EQ) for details.

## Configuration

All Task parameters support Bamboo variable substitution to allow for global, plan or build-specific settings, 
see [Using global, plan or build-specific variables](https://confluence.atlassian.com/x/nwQ_EQ) for details.

* Please note that if the variable key contains the phrase "password", the value will be masked with "\*\*\*\*\*\*\*\*" in the Build Logs. 
E.g. if the key is password, apiKeyPassword or projectTokenPassword the value will be masked.

### Parameters

* _API key_ - Uniquely identifies your organization's White Source account.
* _Project token_ - Uniquely identifies the project built by the job.
* _Includes_ - Ant-style [FileSet](http://ant.apache.org/manual/Types/fileset.html) pattern to determine which files to include as dependencies. Relative to the job's workspace.
* _Excludes_ - Ant-style [FileSet](http://ant.apache.org/manual/Types/fileset.html) pattern to determine which files to exclude as dependencies. Relative to the job's workspace.

### HTTP(S) Proxy

If your Bamboo instance is running behind a firewall, the plugin will reuse the proxy configuration from Bamboo, 
see [Cannot connect to an AWS or EC2 instance when the Bamboo application is running behind a proxy](https://confluence.atlassian.com/x/nAFgDQ) for details.

## License

Licensed under the Apache License, Version 2.0, see LICENSE for details.
