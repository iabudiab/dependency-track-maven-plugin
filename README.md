# Dependency-Track Maven Plugin

[![Build Status](https://travis-ci.org/iabudiab/dependency-track-maven-plugin.svg?branch=master)](https://travis-ci.org/iabudiab/dependency-track-maven-plugin)


[Dependency-Track](https://dependencytrack.org) is an intelligent Software Supply Chain Component Analysis platform that allows organizations to identify and reduce risk from the use of third-party and open source components.

This plugin is modelled after the [Dependency-Track Jenkins Plugin](https://github.com/jenkinsci/dependency-track-plugin) in order to be used in non-Jenkins environments.

Minimum supported Dependency Track version: `3.6.0`


- [Quick Overview](#quick-overview)
- [Usage](#usage)
- [Configuration](#configuration)

# Quick Overview

This plugin can interact with Dependency-Track to perform several security-related actions, e.g. upload/download [CycloneDX SBOMs](https://cyclonedx.org/)
 to/from Dependency-Track, check project metrics, compute diffs etc.

A `Security Gate` can also be configured, in order to fail the build, when a preconfigured number of findings are reported by Dependency-Track.

This plugin also supports custom `suppressions` not configured in Dependency-Track, that filter out specific findings before applying the
security gate.

# Usage

To use the plugin add it into the `build` section of your `pom.xml`. The minimal viable configuration looks as follows:

```xml
<plugin>
    <groupId>dev.iabudiab</groupId>
    <artifactId>dependency-track-maven-plugin</artifactId>
    <version>2.4.1</version>
    <configuration>
        <dependencyTrackUrl>https://dependency-track-installation</dependencyTrackUrl>
        <dependencyTrackApiKey>${env.DTRACK_API_KEY}</dependencyTrackApiKey>
    </configuration>
</plugin>
```

## Configuration

These parameters are required to configure the plugin:

- `dependencyTrackUrl`: The URL where Dependency-Track is hosted.
- `dependencyTrackApiKey`: The API Key for Dependency-Track.

Further you skip the plugin execution with the following configuration:

- `skip`: a boolean value indicating if the plugin should be executed for the current project.

The API Key should have sufficient permissions depending on the performed action:

| Permission                | Description                                                |
|---------------------------|------------------------------------------------------------|
| `BOM_UPLOAD`              | Allows the uploading of CycloneDX and SPDX BOMs            |
| `PROJECT_CREATION_UPLOAD` | Allows the dynamic creation of projects                    |
| `VIEW_VULNERABILITY`      | Allows access to view vulnerabilities                      |
| `VIEW_PORTFOLIO`          | Allows access to view the components portfolio             |


## Suppressions

In order to suppress findings a `JSON` file containing suppression definitions can be configured as follows:

| Parameter     | Description                   | Default Value                          |
|---------------|-------------------------------|----------------------------------------|
| `suppression` | Path to the suppressions file | `${project.basedir}/suppressions.json` |


```json
{
  "suppressions": [
     {
       "by": "purl",
       "purl": "pkg:maven/org.springframework/spring-web@5.3.10?type=jar"
     }
  ]
}
```

Currently, the plugin supports these kinds of suppressions:

- **By PURL**: Suppresses all findings for a given component identified by its `purl`

```json
{
  "by": "purl",
  "purl": "pkg:maven/org.springframework/spring-web@5.3.10?type=jar",
  "regex": false,
  "expiration": "2022-12-23",
  "notes": "Some notes explaining why this was suppressed",
  "state": "NOT_AFFECTED",
  "justification": "REQUIRES_CONFIGURATION",
  "response": "WILL_NOT_FIX"
}
```

- **By CVE**: Suppresses a finding identified by its Vulnerability ID `CVE`

```json
{
  "by": "cve",
  "cve": "CVE-2021-22096",
  "expiration": "2022-12-23",
  "notes": "Some notes explaining why this was suppressed",
  "state": "FALSE_POSITIVE",
  "justification": "NOT_SET",
  "response": "WORKAROUND_AVAILABLE"
}
```

- **By CVE of PURL**: Suppresses a finding identified by its Vulnerability ID `CVE` affecting a specific component identified by its `purl`

```json
{
  "by": "cve-of-purl",
  "cve": "CVE-2021-22096",
  "purl": ".*org.springframework\/spring-web.*",
  "regex": true,
  "expiration": "2022-12-23",
  "notes": "Some notes explaining why this was suppressed",
  "state": "RESOLVED",
  "justification": "NOT_SET",
  "response": "NOT_SET"
}
```

## Goals

- [upload-bom](#upload-bom)
- [download-bom](#download-bom)
- [check-token](#check-token)
- [check-metrics](#check-metrics)
- [diff](#diff)
- [diff-dependency-track](#diff-dependency-track)

### upload-scan

Uploads a Dependency-Check XML Report to Dependency-Track. A project is created in Dependency-Track if it doesn't already exist.

Configuration:

| Parameter                 | Description                                                | Default Value                               |
|---------------------------|------------------------------------------------------------|---------------------------------------------|
| `projectName`             | The unique name of the project in Dependency-Track         | `${project.groupId}.${project.artifactId}`  |
| `projectVersion`          | The version of the project in Dependency-Track             | `${project.version}`                        |
| `artifactDir`             | The directory of the artifact to upload                    | `${project.build.directory}`                |
| `artifactName`            | The name of the artifact to upload                         | `dependency-check-report.xml`               |

---

### upload-bom

Uploads a CycloneDX or SPDX BOM to Dependency-Track. A project is created in Dependency-Track if it doesn't already exist.

If either `parentIdentifier` or both of `parentName` and `parentVersion` are specified (not empty), the appropriate parent project will be applied as a parent to the target project. If `autoCreateParent` is set to `true` and no matching parent project found in Dependency-Track, an appropriate parent project will be created first, otherwise the assigned parent project will be left as is. 

Upon uploading a BOM to Dependency-Track a token is returned, which can be checked for processing status if `pollToken` is `true`.

If `pollToken` is set to `false`, then this goal would upload the BOM, optionally assign the specified parent project, write the token to a file at `tokenFile` and then exit.

Once the token is processed, the findings are available and can be fetched for further analysis.

If `setOlderVersionsInactive` is set to `true` older versions (those that are lower than or equal) of this project will be set to `inactive` in Dependency-Track. To identify 'older' versions a three numbered version scheme (`[major-version].[minor-version].[patch-version]`, e.g. 3.11.8 or 1.2 or 2) is assumed and will be checked with an appropriate regex. If the version does not match that scheme, the corresponsing project version will be skipped. Here Older versions means all versions that have
1. major-version less than current major-version or equal and
2. minor-version less than current minor-version or equal and
3. patch-version less than current patch-version

When `ignoreVersionSuffixes` is set to true all suffixes to prior defined versioning scheme will be ignored when it comes to comparing the other projects version to the curent one. As a result, all versions with the same base versions will be set to "inactive", e.g. If we are releasing a version 1.2.3 all versions like 1.2.3-SNAPSHOT or 1.2.3-beta.2 will be set to "inactive" as well as all lower versions like 1.2.1 or 1.2 and so on.

This goal polls Dependency-Track for `tokenPollingDuration`, which defaults to `60` seconds, then prints a findings report. The findings can be matched against a `security gate` in order to fail the build, which can be configured as follows:


```xml
<plugin>
    ...
    <configuration>
        <securityGate>
            <critical>1</critical>
            <high>2</high>
            <medium>3</medium>
            <low>4</low>
        </securityGate>
    </configuration>
    ...
</plugin>
```

- If a matching suppression found for a finding returned from server and `uploadMatchingSuppressions` is set to `true`, it will be suppressed in Dependency-Track server too by using the provided information.
- If a matching suppression found for a finding returned from server and `resetExpiredSuppressions` is set to `true`, then the corresponding Analysis is reset in Dependency-Track server, when the local suppression expires.

Configuration:

| Parameter                    | Description                                                                         | Default Value                                                                 |
|------------------------------|-------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| `projectName`                | The unique name of the project in Dependency-Track                                  | `${project.groupId}.${project.artifactId}`                                    |
| `projectVersion`             | The version of the project in Dependency-Track                                      | `${project.version}`                                                          |
| `artifactDir`                | The directory of the artifact to upload                                             | `${project.build.directory}`                                                  |
| `artifactName`               | The name of the artifact to upload                                                  | `bom.xml`                                                                     |
| `pollToken`                  | Whether to poll the pending token for processing or not                             | `true`                                                                        |
| `tokenFile`                  | The file path into which the token will be written                                  | `${project.build.directory}/dependency-track/pendingToken`                    |
| `tokenPollingDuration`       | Polling timeout for the uploaded BOM token.                                         | `60` seconds                                                                  |
| `projectMetricsRetryDelay`   | Delay between each retry requesting project metrics                                 | `5` seconds                                                                   |
| `projectMetricsRetryLimit`   | Maximum number of retries requesting project metrics                                | `3` times <br/>                                                               |
| `securityGate`               | The security gate configuration                                                     | <ul><li>critial: 0</li><li>high: 0</li><li>medium: 0</li><li>low: 0</li></ul> |
| `uploadMatchingSuppressions` | Whether to upload matching suppression or not	                                     | `false`                                                                       |
| `resetExpiredSuppressions`   | Whether to reset matching expired suppression or not                                | `true`                                                                        |
| `cleanupSuppressions`        | Whether to generate a cleaned up suppressions file without unnecessary suppressions | `true`                                                                        |
| `cleanupSuppressionsFile`    | The file path into which the suppressions will be written                           | `${project.build.directory}/dependency-track/suppressions.json `              |
| `parentIdentifier`           | The unique identifier (UUID) of the parent project in Dependency-Track              | empty                                                                         |
| `parentName`                 | The unique name of the parent project in Dependency-Track                           | empty                                                                         |
| `parentVersion`              | The version of the parent project in Dependency-Track                               | empty                                                                         |
| `autoCreateParent`           | Whether to create or not the specified parent project if no such project exists     | `false`                                                                       |
| `parentCollectionLogic`      | The collection logic that should be applied to the created parent project           | empty                                                                         |
| `parentCollectionTag`        | The collection tag that should be applied to the created parent project             | empty                                                                         |
| `collectionLogic`            | The collection logic that should be applied to the current project                  | empty                                                                         |
| `collectionTag`              | The collection tag that should be applied to the current project                    | empty                                                                         |
| `setOlderVersionsInactive`   | Whether or not set older versions of this project to 'inactive' in Dependency-Track | `false`                                                                       |
| `ignoreVersionSuffixes`      | Whether or not ignore version suffixes when identifying old versions                | `true`                                                                        |

---

### download-bom

Downloads a project's CycloneDX SBoM from Dependency Track.

Configuration:

| Parameter          | Description                                                                   | Default Value                                                   |
|--------------------|-------------------------------------------------------------------------------|-----------------------------------------------------------------|
| `projectName`      | The unique name of the project in Dependency-Track                            | `${project.groupId}.${project.artifactId}`                      |
| `projectVersion`   | The version of the project in Dependency-Track                                | `${project.version}`                                            |
| `destinationPath`  | The destination directory where the BOM will be downloaded                    | `${project.build.directory}/${project.build.finalName}_bom.xml` |
| `outputFormat`     | The format of the BOM to download from Dependency-Track (**XML** or **JSON**) | XML                                                             |
---

### check-token

Polls a token for processing status and applies a `SecurityGate` on the fetched findings.

The token value can be either read from a file via the `tokenFile` or passed directly as `tokenValue` property.

If both are set then `tokenValue` takes precedence over `tokenFile`.

Configuration:

| Parameter                 | Description                                                | Default Value                                                                   |
|---------------------------|------------------------------------------------------------|---------------------------------------------------------------------------------|
| `projectName`             | The unique name of the project in Dependency-Track         | `${project.groupId}.${project.artifactId}`                                      |
| `projectVersion`          | The version of the project in Dependency-Track             | `${project.version}`                                                            |
| `tokenFile`               | The file path into which the token will be written         | `${project.build.directory}/dependency-track/pendingToken`                      |
| `tokenValue`              | The UUID value of the pending token                        |                                                                                 |
| `tokenPollingDuration`    | Polling timeout for the uploaded BOM token.                | `60` seconds                                                                    |
| `securityGate`            | The security gate configuration                            | <ul><li>critial: 0</li><li>high: 0</li><li>medium: 0</li><li>low: 0</li></ul>   |

---

### check-metrics

Checks a project's current metrics and applies a `SecurityGate` on any current findings.

Configuration:

| Parameter                 | Description                                                | Default Value                                                                   |
|---------------------------|------------------------------------------------------------|---------------------------------------------------------------------------------|
| `projectName`             | The unique name of the project in Dependency-Track         | `${project.groupId}.${project.artifactId}`                                      |
| `projectVersion`          | The version of the project in Dependency-Track             | `${project.version}`                                                            |
| `securityGate`            | The security gate configuration                            | <ul><li>critial: 0</li><li>high: 0</li><li>medium: 0</li><li>low: 0</li></ul>   |

---

### diff

Computes a diff between two local BOM files and outputs the results.

The resulting diff indicates for each component whether it is `added`, `removed` or stays `unchanged` if the `first` BOM is applied to the `second`.

This goal mimics the behaviour of the same command in [cyclonedx-cli](https://github.com/CycloneDX/cyclonedx-cli).

- The **JSON** output format produces the same results as the `cyclonedx-cli`, which can be written to a destination file.
- The **TEXT** output format produces the same textual output in stdout as  `cyclonedx-cli`.

Configuration:

| Parameter      | Description                                                         | Default Value                           |
|----------------|---------------------------------------------------------------------|-----------------------------------------|
| `from`         | The path of the first BOM for comparison                            |                                         |
| `to`           | The path of the second BOM for comparison                           |                                         |
| `outputFormat` | The format of the result output. Can be either **JSON** or **TEXT** | JSON                                    |
| `outputPath`   | The path of the output file                                         | `${project.build.directory}/diff.json`  |

---

### diff-dependency-track

Computes a diff between a local BOM file and its counterpart of the corresponding project in Dependency-Track. This can be used,
for example, to check if the local state differs from the last imported BOM in Dependency-Track and act accordingly.


If the project doesn't yet exist in Dependency-Track, then the local BOM is compared with a dummy empty BOM, which would result
in a diff indicating that all the components in the local BOM would be `added` if applied.

Configuration:

| Parameter       | Description                                                         | Default Value                           |
|-----------------|---------------------------------------------------------------------|-----------------------------------------|
| `localBomPath`  | The path of the local BOM                                           | `${project.build.directory}/bom.xml`    |
| `outputFormat`  | The format of the result output. Can be either **JSON** or **TEXT** | JSON                                    |
| `outputPath`    | The path of the output file                                         | `${project.build.directory}/diff.json`  |

### Summary

Here are all the main configuration parameters summarized:

| Parameter                    | Description                                              | Default Value                                                                                            |
|------------------------------|----------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| `dependencyTrackUrl`         | The URL of the Dependency-Track Server                   |                                                                                                          |
| `dependencyTrackApiKey`      | An API key for Dependency-Track                          |                                                                                                          |
| `skip`                       | Skip plugin execution for the current project            | `false`                                                                                                  |
| `failOnError`                | Whether errors should fail the build                     | `true`                                                                                                   |
| `logPayloads`                | Whether the plugin should log request/response payloads  | `false`                                                                                                  |
| `projectName`                | The unique name of the project in Dependency-Track       | `${project.groupId}.${project.artifactId}`                                                               |
| `projectVersion`             | The version of the project in Dependency-Track           | `${project.version}`                                                                                     |
| `artifactDir`                | The directory of the artifact to upload                  | `${project.build.directory}`                                                                             |
| `artifactName`               | The name of the artifact to upload                       | <ul><li>`upload-scan` goal: `dependency-check-report.xml`</li><li>`upload-bom` goal: `bom.xml`</li></ul> |
| `tokenPollingDuration`       | Polling timeout for the uploaded BOM token.              | `60` seconds                                                                                             |
| `projectMetricsRetryDelay`   | Delay between each retry requesting project metrics      | `5` seconds                                                                                              |
| `projectMetricsRetryLimit`   | Maximum number of retries requesting project metrics     | `3` times                                                                                                |
| `securityGate`               | The security gate configuration                          | <ul><li>critial: 0</li><li>high: 0</li><li>medium: 0</li><li>low: 0</li></ul>                            |
| `suppressions`               | Path to the suppressions file                            | `${project.basedir}/suppressions.json`                                                                   |


# License

This plugin "Dependency-Track Maven Plugin" is available under the Apache License 2.0. See the [LICENSE](LICENSE) file for more info.

This is an independent project and not affiliated with, endorsed nor sponsored by OWASP, Dependency-Check, Dependency-Track, CycloneDX, or SPDX. Any product names, logos, brands, and other trademarks used in this project are the property of their respective trademark holders. These trademark holders are not affiliated with this project.
