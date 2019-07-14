# Dependency-Track Maven Plugin

[![Build Status](https://travis-ci.org/iabudiab/dependency-track-maven-plugin.svg?branch=master)](https://travis-ci.org/iabudiab/dependency-track-maven-plugin)


[Dependency-Track](https://dependencytrack.org) is an intelligent Software Supply Chain Component Analysis platform that allows organizations to identify and reduce risk from the use of third-party and open source components.

This plugin is modelled after the [Dependency-Track Jenkins Plugin](https://github.com/jenkinsci/dependency-track-plugin) in order to be used in non-Jenkins environments.


- [Quick Overview](#quickoverview)
- [Usage](#usage)
- [Configuration](#configuration)

# Quick Overview

This plugin can upload [Dependency-Check XML Reports](https://jeremylong.github.io/DependencyCheck/), [CycloneDX SBOMs](https://cyclonedx.org/), and [SPDX SBOMs](https://spdx.org/) to Dependency-Track.

A `Security Gate` can also be configured, in order to fail the build, when a preconfigured number of findings are reported by Dependency-Track.

# Usage

To use the plugin add it into the `build` section of your `pom.xml`. The minimal viable configuration looks as follows:

```xml
<plugin>
    <groupId>dev.iabudiab</groupId>
    <artifactId>dependency-track-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <dependencyTrackUrl>https://dependency-track-installation</dependencyTrackUrl>
        <dependencyTrackApiKey>${env.DTRACK_API_KEY}</dependencyTrackApiKey>
    </configuration>
</plugin>
```

## Goals

- `upload-scan`: Uploads a Dependency-Check XML Report to Dependency-Track. A project is created in Dependency-Track if it doesn't already exist.

Configuration:

| Parameter                 | Description                                                | Default Value |
|---------------------------|------------------------------------------------------------|---------------|
| `projectName`             | The unique name of the porject in Dependency-Track         | `${project.groupId}.${project.artifactId}` |
| `projectVersion`          | The version of the project in Dependency-Track             | `${project.version}` |
| `artifactDirectory`       | The directory of the artifact to upload                    | `${project.build.directory}` |
| `artifactName`            | The name of the artifact to upload                         | `dependency-check-report.xml` |

---

- `upload-bom`: Uploads a CycloneDX or SPDX BOM to Dependency-Track. A project is created in Dependency-Track if it doesn't already exist.

Upon uploading a BOM to Dependency-Track a token is returned, which can be checked for processing status. Once the token is processed, the findings are available and can be fetched for further analysis.

This goal polls Dependency-Track for `tokenPollingDuration`, which defaults to `60` seconds, then prints a findings report. The findings can be matched against a `security gate` in order to fail the build, which can be configured as follows:


```xml
<plugin>
    ...
    <configuration>
        <securityGaye>
            <critical>1</critical>
            <high>2</high>
            <medium>3</medium>
            <low>4</low>
        </securityGaye>
    </configuration>
    ...
</plugin>
```

Configuration:

| Parameter                 | Description                                                | Default Value |
|---------------------------|------------------------------------------------------------|---------------|
| `projectId`               | The UUID of the project in Dependency-Track.               |  |
| `projectName`             | The unique name of the porject in Dependency-Track         | `${project.groupId}.${project.artifactId}` |
| `projectVersion`          | The version of the project in Dependency-Track             | `${project.version}` |
| `artifactDirectory`       | The directory of the artifact to upload                    | `${project.build.directory}` |
| `artifactName`            | The name of the artifact to upload                         | `bom.xml` |
| `tokenPollingDuration`    | Polling timeout for the uploaded BOM token.                | `60` seconds |
| `securityGate`            | The security gate configuration                            | <ul><li>critial: 0</li><li>high: 0</li><li>medium: 0</li><li>low: 0</li></ul> |

## Configuration

These parameters are requried:

- `dependencyTrackUrl`: The URL where Dependency-Track is hosted.
- `dependencyTrackApiKey`: The API Key for Dependency-Track.

The API Key should have suffiecien permissions depending on the performed action:

| Permission                | Description                                                |
|---------------------------|------------------------------------------------------------|
| `BOM_UPLOAD`              | Allows the uploading of CycloneDX and SPDX BOMs            |
| `SCAN_UPLOAD`             | Allows the uploading of Dependency-Check XML reports       |
| `VULNERABILITY_ANALYSIS`  | Allows access to the findings API for trending and results |
| `PROJECT_CREATION_UPLOAD` | Allows the dynamic creation of projects                    |

- `upload-scan`: Required permissions are `SCAN_UPLOAD` & `PROJECT_CREATION_UPLOAD`
- `upload-bom` : Required permissions are `BOM_UPLOAD` & `VULNERABILITY_ANALYSIS` & `PROJECT_CREATION_UPLOAD`

### Summary

Here are all the configuration parameters summerized:

| Parameter                 | Description                                                | Default Value |
|---------------------------|------------------------------------------------------------|---------------|
| `dependencyTrackUrl`      | The URL of the Dependency-Track Server                     |               |
| `dependencyTrackApiKey`   | An API key for Dependency-Track                            |               |
| `failOnError`             | Whether errors should fail the build                       | `true`        |
| `projectId`               | The UUID of the project in Dependency-Track. Required only for security-gate feature |  |
| `projectName`             | The unique name of the porject in Dependency-Track         | `${project.groupId}.${project.artifactId}` |
| `projectVersion`          | The version of the project in Dependency-Track             | `${project.version}` |
| `artifactDirectory`       | The directory of the artifact to upload                    | `${project.build.directory}` |
| `artifactName`            | The name of the artifact to upload                         | <ul><li>`upload-scan` goal: `dependency-check-report.xml`</li><li>`upload-bom` goal: `bom.xml`</li></ul> |
| `tokenPollingDuration`    | Polling timeout for the uploaded BOM token.                | `60` seconds |
| `securityGate`            | The security gate configuration                            | <ul><li>critial: 0</li><li>high: 0</li><li>medium: 0</li><li>low: 0</li></ul> |


# License

This plugin "Dependency-Track Maven Plugin" is available under the Apache License 2.0. See the [LICENSE](LICENSE) file for more info.

This is an independent project and not affiliated with, endorsed nor sponsored by OWASP, Dependency-Check, Dependency-Track, CycloneDX, or SPDX. Any product names, logos, brands, and other trademarks used in this project are the property of their respective trademark holders. These trademark holders are not affiliated with this project.
