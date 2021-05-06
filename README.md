<img src="doc/images/greenish-with-background.svg" width="200" height="200"/>

[![Build Status](https://travis-ci.org/amanjpro/greenish.svg?branch=master)](https://travis-ci.org/amanjpro/greenish)
[![codecov](https://codecov.io/gh/amanjpro/greenish/branch/master/graph/badge.svg)](https://codecov.io/gh/amanjpro/greenish) [![Join the chat at https://gitter.im/greenish-monitoring/greenish](https://badges.gitter.im/greenish-monitoring/greenish.svg)](https://gitter.im/greenish-monitoring/greenish?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

**Greenish** is a monitoring tool that checks datasets for existence.

Greenish understands _periods;_ for example, for an hourly job, it can
verify that all datasets for the past _N_ hours exist.

Configuration files use the [HOCON] syntax (a superset of [JSON];
similar to [YAML]):

* [annotated example](src/test/resources/application.conf);
* [default values](src/main/resources/reference.conf).

[HOCON]: https://github.com/lightbend/config/blob/master/HOCON.md
[JSON]:  https://en.wikipedia.org/wiki/JSON
[YAML]:  https://en.wikipedia.org/wiki/YAML


Greenish runs [monitoring jobs] to collect information about which
datasets are available are missing. Those are individual scripts that
can be written in any language.

[monitoring jobs]: (#monitoring-jobs)


## Greenish dashboard

Greenish provides a basic HTML dashboard to visualise the state of the
monitored jobs. The dashboard can be accessed at `/dashboard`.

Here is a screenshot:

![Greenish dashboard screenshot](doc/images/dashboard.png)

## API

[The Greenish API is documented in `api.md`.](doc/api.md)

## Who uses Greenish?

Greenish is still new. As of now, [Samsung
Ads](https://www.samsung.com/us/business/samsungads/) uses Greenish to monitor
_business-critical datasets_.

## Greenish vs others

*  **Nagios** is a monitoring tool for systems, network and
   infrastructure. It is very good to keep track of the instantaneous
   state of a system. But it has no notion of datasets that follow a
   periodic pattern (e.g., daily jobs or hourly jobs). Making Nagios
   aware of periods is entirely on the shoulder of the check writers,
   which can be very tricky to do (or even impossible?).

*  **Prometheus** is another great tool for monitoring metrics, and the
   health of other systems, but again it doesn't know about datasets
   that follow periodic patterns. It is worth mentioning that Greenish
   provides an endpoint to export metrics to Prometheus.

*  **Airflow** knows about periods, but it is not a monitoring
   tool. Airflow can alert when a run fails, but if an existing dataset
   gets deleted accidentally, Airflow stays unaware.

What sets Greenish apart is that it knows about periods, and keeps checking
datasets for existence.

## Monitoring Jobs

As mentioned earlier, monitoring scripts are stand-alone programs,
written in any language, that respect the following contract:

* The scripts must be executable.

* The scripts must accept an arbitrary number of `period` arguments at
  the end of their parameter list; e.g., for a script named
  `monitor-foo`, running on the `staging` environment, asked to check
  the status of three hourly periods:

  ```shell
  monitor-foo staging 2020-20-06-10 2020-20-06-11 2020-20-06-12
  ```

  The `check-command` entry for the example above could be:

  ```yaml
    check-command: "monitor-foo staging"
    period-pattern: "yyyy-MM-dd-HH"
  ```

- The scripts must print one diagnostic line per provided period in
  one of the following two formats, where `1` indicates a successful
  period, and `0` indicates a failed period:

  ```text
  greenish-period <tab> <period> <tab> 0
  greenish-period <tab> <period> <tab> 1
  ```

  Where:

  * Each value for `<period>` must match one of the periods passed to
    the monitoring script.

  * Diagnostic lines are recognized by regular expression
    `^greenish-period\t.*\t(0|1)$`.

  * Any lines not matching the format are ignored by Greenish. This
    allows monitoring scripts to print extra debugging data.

- The scripts must exit with 0, regardless of the status of any
  individual check. Exiting in error is reserved for problems
  evaluating the checks themselves.

Example monitoring script:

```
#!/usr/bin/env bash
farm=$1; shift

echo '# Start of checks'
for period in "$@"; do
  echo '# Arbitrary debugging info here'

  ## Note how the `ls` command below does print some output, which
  ## Greenish will ignore. (Unless the input directory is malicious,
  ## and purposefully includes files named in the way that Greenish
  ## expects as representing check output.)
  if ls "$farm/$period"; then
    printf 'greenish-period\t%s\t%d\n' "$period" 1
  else
    printf 'greenish-period\t%s\t%d\n' "$period" 0
  fi
done
```

## Performance Tweaking

The monitoring jobs are usually blocking IO jobs. Do that network call, wait
for this API, connect to a DB, HDFS etc. That is why they are running under
their very own execution context (thread pool). So that they do not block the
rest of the service (namely the endpoints). The execution context config for
the monitoring jobs are controlled by a dispatcher named `refresh-dispatcher`.
Greenish comes with a default config that is suitable for IO-bound processes,
you can find it in the default settings mentioned earlier.

It is best to use `thread-pool-executor` dispatcher for blocking jobs, as they
are tailored for IO jobs. More information can be found:

- [ThreadPoolExecutor Javadoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadPoolExecutor.html)
- [Akka documentaiton](https://doc.akka.io/docs/akka-http/current/handling-blocking-operations-in-akka-http-routes.html#solution-dedicated-dispatcher-for-blocking-operations)

## Logging

Greenish uses Akka's simple logging mechanism. In the spirit of [12 factor
App](https://12factor.net/logs) all logs are written to STDOUT, and the
configuration can be done via the `application.conf` file. The following
is a summary of some of the most useful options for customizing logging:

```
akka {
  # Log the complete configuration at INFO level when Greenish is started.
  # This is useful when you are uncertain of what configuration is used.
  log-config-on-start = on
  # Options are: OFF, DEBUG, INFO, ERROR, WARN
  loglevel = "DEBUG"
  # To turn off logging completely
  stdout-loglevel = "OFF"

  # Not necessarily useful in prod, but can be useful during development
  # You probably want to skip the following in produciton
  log-dead-letters = 10
  log-dead-letters-during-shutdown = on
  actor {
    debug {
      # enable function of LoggingReceive, which is to log any received message at
      # DEBUG level
      receive = on
      # enable DEBUG logging of all AutoReceiveMessages (Kill, PoisonPill etc.)
      autoreceive = on
      # enable DEBUG logging of actor lifecycle changes
      lifecycle = on
      # enable DEBUG logging of unhandled messages
      unhandled = on
      # enable DEBUG logging of all LoggingFSMs for events, transitions and timers
      fsm = on
    }
  }
}
```

## Pre-built package

You can download pre-built packages (both fat (i.e. assembly) jar and docker)
from the [releases page](https://github.com/amanjpro/greenish/releases). The
latest docker image can be found at the [packages
page](https://github.com/amanjpro/greenish/packages).

## Development

### Requirements

- Java 8
- SBT 1.3.x
- Bash
- NodeJS 14+

### Building from the source

First install `npm` dependencies:

`$ npm install`

SBT takes care of building/testing both the Scala and JavaScript/JSX:

`$ sbt clean test package`

To run the service from the source:
`$ sbt -Dconfig.file=PATH_TO_CONFIG_FILE run`

**Note** Unfortunately, the JavaScript code has no tests yet, this is an issue
that needs to be resolved.

#### Packaging

Greenish supports both "fat jar" and docker. Fat jar is a single and
self-contained jar that can be distributed on any *nix environment (as long as
Java and Bash are installed):

```
$ sbt assembly
$ java -Dconfig.file=PATH_TO_CONFIG_FILE -jar target/scala-2.13/greenish-assembly-*.jar
```

You can also build docker images:

```
$ sbt docker:publishLocal
# The docker image expects config to be mounted at: /app/config.yml
$ docker run --volume PATH_TO_CONFIG_FILE:/app/config.yml --rm -p 8080:8080 greenish:LATEST_VERSION
```

## Contributing

Contributions are most welcome. Please, fork it, use it, open issues and submit PRs!

## Acknowledgment

- Thanks to [Nasrin Zaza](https://www.linkedin.com/in/nasrin-zaza/) for the
  amazing logo
