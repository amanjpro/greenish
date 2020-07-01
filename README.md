# Greenish

[![Build Status](https://travis-ci.org/amanjpro/greenish.svg?branch=master)](https://travis-ci.org/amanjpro/greenish)
[![codecov](https://codecov.io/gh/amanjpro/greenish/branch/master/graph/badge.svg)](https://codecov.io/gh/amanjpro/greenish)

Greenish is a monitoring tool, that can be used to monitor the presense of
data-sets. Greenish understands periods, for example for an hourly job,
Greenish can check for data for all the past _N_ hour data-sets.

Greenish jobs are configured in a
[YAML-like](https://github.com/lightbend/config) configuration file, [here is
an annotated example](src/test/resources/application.conf). Greenish does not
have a standard monitoring scripting language/plugin. Monitoring tasks can be
in any executable form, as long as, they take `period` as the last parameter,
and they exit with 0 only upon success. Here is a very simple example for a
script to check if data has arrived:

```
#!/usr/bin/env bash

# $2 is the period
ls "/var/log/$1/$2"
```

We can use _the above script_, like this:

```
  cmd: "THE_ABOVE_SCRIPT my_job"
```

Note, how we provide the first argument, but expect Greenish to provide the
second--i.e. the period.

The example config (linked above), explains the possible configurations in
detail.

## The API

Greenish supports four REST endpoints:

### Display the maximum number of missing datasets 

Basically, for all the jobs, find the job that misses the most number of
period datasets, and return the value.

```
$ curl --silent http://0.0.0.0:8080/maxlag | jq .
{
  "lag": 0
}
```
### Summary

Display the summary of all the monitoring tasks. Very good for a quick glance:

```
$ curl --silent http://0.0.0.0:8080/summary | jq .
[
  {
    "name": "Group1",
    "status": [
      {
        "name": "Job1",
        "missing": 4,
        "alert_level": "warn"
      },
      {
        "name": "Job2",
        "missing": 2,
        "alert_level": "normal"
      }
    ]
  },
  {
    "name": "Group2",
    "status": [
      {
        "name": "Job3",
        "missing": 6,
        "alert_level": "critical"
      },
      {
        "name": "Job4",
        "missing": 0,
        "alert_level": "great"
      }
    ]
  }
]
```

### Display all the periods that are missing for all the jobs

```
$ curl --silent http://0.0.0.0:8080/missing | jq .
[
  {
    "group": {
      "group_id": 0,
      "name": "Group1",
      "jobs": [
        {
          "job_id": 0,
          "name": "Job1",
          "cmd": "/tmp/first_script",
          "time_pattern": "yyyy-MM-dd-HH",
          "frequency": "hourly",
          "timezone": {
            "zone_id": "UTC"
          },
          "lookback": 24,
          "alert_levels": {
            "great": 0,
            "normal": 1,
            "warn": 2,
            "critical": 3
          }
        }
      ]
    },
    "status": [
      {
        "job": {
          "job_id": 0,
          "name": "Job1",
          "cmd": "/tmp/first_script",
          "time_pattern": "yyyy-MM-dd-HH",
          "frequency": "hourly",
          "timezone": {
            "zone_id": "UTC"
          },
          "lookback": 24,
          "alert_levels": {
            "great": 0,
            "normal": 1,
            "warn": 2,
            "critical": 3
          }
        },
        "updated_at": 1593567901,
        "period_health": [
          {
            "period": "2020-06-27-20",
            "ok": false
          }

      ...
```

### Display the current state

A very detailed view for all monitoring tasks:

```
$ curl --silent http://0.0.0.0:8080/state | jq .
[
  {
    "group": {
      "group_id": 0,
      "name": "Group1",
      "jobs": [
        {
          "job_id": 0,
          "name": "Job1",
          "cmd": "/tmp/first_script",
          "time_pattern": "yyyy-MM-dd-HH",
          "frequency": "hourly",
          "timezone": {
            "zone_id": "UTC"
          },
          "lookback": 24,
          "alert_levels": {
            "great": 0,
            "normal": 1,
            "warn": 2,
            "critical": 3
          }
        }
      ]
    },
    "status": [
      {
        "job": {
          "job_id": 0,
          "name": "Job1",
          "cmd": "/tmp/first_script",
          "time_pattern": "yyyy-MM-dd-HH",
          "frequency": "hourly",
          "timezone": {
            "zone_id": "UTC"
          },
          "lookback": 24,
          "alert_levels": {
            "great": 0,
            "normal": 1,
            "warn": 2,
            "critical": 3
          }
        },
        "updated_at": 1593567901,
        "period_health": [
          {
            "period": "2020-06-27-20",
            "ok": true
          },
          {
            "period": "2020-06-27-21",
            "ok": true
          },

        ...
```

An HTML dashboard is expected to land at `/dashboard` in the future releases.

## Pre-built package

You can download pre-built packages (both fat(assembly) jar and docker) from
the [releases page](https://github.com/amanjpro/greenish/releases).

## Development

### Requirements

- Java 8
- SBT 1.3.x

### Building from the source

SBT takes care of building/testing:

`$ sbt clean test package`

To run the service from the source:
`$ sbt -Dconfig.file=PATH_TO_CONFIG_FILE run`

#### Packaging

Greenish supports both "fat jar", that is a single and self-contained jar that
can be distributed and run everwhere (as long as Java is installed):

```
$ sbt assembly
$ java -Dconfig.file=PATH_TO_CONFIG_FILE target/scala-2.13/greenish-assembly-*.jar
```

You can also build docker images:

```
$ sbt docker:publishLocal
# The docker image expects config to be mounted at: /app/config.yml
$ docker run --volume PATH_TO_CONFIG_FILE:/app/config.yml --rm -p 8080:8080 greenish:LATEST_VERSION
```

## Contributing

Contributions are most welcome. Please, fork it, use it, open issues and submit PRs!

## Known issues

As of now, the task that refreshes the monitoring state, waits for all the
monitoring scripts for all periods to finish their checks. That means, Greenish
still doesn't support partial state updates, adding it should not be too difficutl,
but requires some caution. Contributions are more than welcome.
