# Greenish

[![Build Status](https://travis-ci.org/amanjpro/greenish.svg?branch=master)](https://travis-ci.org/amanjpro/greenish)
[![codecov](https://codecov.io/gh/amanjpro/greenish/branch/master/graph/badge.svg)](https://codecov.io/gh/amanjpro/greenish)

Data monitoring tool. Do not monitor the runs, monitor the results.

## Building

### Sbt-Assembly

```
$ sbt assembly
$ java -Dconfig.file=config-sample.yml target/scala-2.13/greenish-assembly-*.jar
```


### Docker

```
$ sbt docker:publishLocal
$ docker run --volume $(pwd)/config-sample.yml:/app/config.yml --rm -p 8080:8080 greenish:LATEST_VERSION
```
