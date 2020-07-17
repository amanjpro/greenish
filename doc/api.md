# The API

## REST

Greenish provides a few REST endpoints:

### Display the maximum number of missing datasets 

Basically, for all the jobs, find the job that misses the most number of
period datasets, and return the number.

```
$ curl --silent -G http://0.0.0.0:8080/maxlag | jq .
{
  "lag": 0
}
```

### Summary

Display the summary of all the monitoring tasks. Very good for a quick glance:

```
$ curl --silent -G http://0.0.0.0:8080/summary | jq .
[
  {
    "group_id": 0,
    "name": "Group1",
    "status": [
      {
        "job_id": 0,
        "name": "Job1",
        "missing": 4,
        "alert_level": "warn"
      },
      {
        "job_id": 1,
        "name": "Job2",
        "missing": 2,
        "alert_level": "normal"
      }
    ]
  },
  {
    "group_id": 0,
    "name": "Group2",
    "status": [
      {
        "job_id": 0,
        "name": "Job3",
        "missing": 6,
        "alert_level": "critical"
      },
      {
        "job_id": 1,
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
$ curl --silent -G http://0.0.0.0:8080/missing | jq .
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
          env: []
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
          env: []
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
$ curl --silent -G http://0.0.0.0:8080/state | jq .
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
          env: []
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
          env: []
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

### Get job and group by id

You can query a single group by its id:

```
$ curl --silent -G localhost:8080/group/1 | jq .
{
  "group": {
    "group_id": 1,
    "name": "Group2",
    "jobs": [
      {
        "job_id": 0,
        "name": "Job3",
        "cmd": "/tmp/third_script",
        "time_pattern": "yyyy-MM-dd",
        "frequency": "monthly",
        "timezone": {
    ...
```

You can also focus on a single job, and query it:

```
$ curl --silent -G localhost:8080/group/1/job/0 | jq .
{
  "job": {
    "job_id": 0,
    "name": "Job3",
    "cmd": "/tmp/third_script",
    "time_pattern": "yyyy-MM-dd",
    "frequency": "monthly",
    "timezone": {
      "zone_id": "UTC"
    },
    "lookback": 3,
    "alert_levels": {
      "great": 0,
      "normal": 1,
      "warn": 2,
      "critical": 3
    }
    env: []
  },
  "updated_at": 1593585049298,
  "period_health": [
    {
      "period": "2020-05-01",
      "ok": true
    },
    {
      "period": "2020-06-01",
      "ok": true
    },
    {
      "period": "2020-07-01",
      "ok": true
    }
  ]
}
```

### Refresh the state

You can refresh the entire at once:

```
$ curl --silent -G localhost:8080/state/refresh | jq .
{
  "ok": "State refresh is scheduled"
}
```

You can point refresh the state of a single group by its id:

```
$ curl --silent -G localhost:8080/group/0/refresh | jq .
{
  "ok": "Group status refresh is scheduled"
}
```

You can also point refresh the state of a single job by its id:

```
$ curl --silent -G localhost:8080/group/0/job/0/refresh | jq .
{
  "ok": "Job status refresh is scheduled"
}
```
### Health-check

Checks if any of the last 5 state refreshes succeeded, if yes, then it is
considered a good health.

```
$ curl --silent -G http://0.0.0.0:8080/health | jq .
{
  "health": "good"
}
```

## Prometheus

Greenish can also export data to Prometheus. These are the supported metrics:

```
TYPE: GAUGE
NAME: greenish_active_refresh_tasks
HELP: Current number active state refresh tasks
LABELS: job_id

TYPE: HISTOGRAM
NAME: greenish_state_refresh_time_seconds
HELP: Job state refreshing time
LABELS: job_id

TYPE: COUNTER
NAME: greenish_state_refresh_total
HELP: Total number of job state refresh instances
LABELS: job_id

TYPE: COUNTER
NAME: greenish_state_refresh_failed_total
HELP: Total number of failed job state refresh instances
LABELS: job_id

TYPE: GAUGE
NAME: greenish_missing_periods_total
HELP: Current number of missing dataset periods
LABELS: job_id
```

Prometheus metrics can be accessed at `/prometheus` endpoint:

```
$ curl --silent -G localhost:8080/prometheus
# HELP greenish_active_refresh_tasks Current number active state refresh tasks
# TYPE greenish_active_refresh_tasks gauge
greenish_active_refresh_tasks{job_id="job_2",} 1.0
greenish_active_refresh_tasks{job_id="job_1",} 0.0
greenish_active_refresh_tasks{job_id="job_4",} 1.0
greenish_active_refresh_tasks{job_id="job_3",} 1.0
...
```
