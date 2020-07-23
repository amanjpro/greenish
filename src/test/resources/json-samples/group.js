module.exports = {
  "testObject": {
    {
      "group": {
        "group_id": 0,
        "name": "Reporting Hourly Jobs",
        "jobs": [
          {
            "job_id": 0,
            "name": "Raw event logs",
            "prometheus_id": "reporting_hourly_jobs_raw_event_logs",
            "cmd": "/tmp/check /tmp",
            "time_pattern": "yyyy-MM-dd-HH",
            "frequency": "hourly",
            "period_check_offset": 1,
            "timezone": {
              "zone_id": "UTC"
            },
            "lookback": 6,
            "alert_levels": {
              "great": 0,
              "normal": 1,
              "warn": 2,
              "critical": 3
            },
            "env": []
          },
          {
            "job_id": 1,
            "name": "Processed logs",
            "prometheus_id": "reporting_hourly_jobs_processed_logs",
            "cmd": "/tmp/check /tmp",
            "time_pattern": "yyyy-MM-dd-HH",
            "frequency": "hourly",
            "period_check_offset": 1,
            "timezone": {
              "zone_id": "UTC"
            },
            "lookback": 6,
            "alert_levels": {
              "great": 1,
              "normal": 2,
              "warn": 3,
              "critical": 4
            },
            "env": []
          },
          {
            "job_id": 2,
            "name": "Summary metrics",
            "prometheus_id": "reporting_hourly_jobs_summary_metrics",
            "cmd": "/tmp/check /tmp",
            "time_pattern": "yyyy-MM-dd-HH",
            "frequency": "hourly",
            "period_check_offset": 1,
            "timezone": {
              "zone_id": "UTC"
            },
            "lookback": 6,
            "alert_levels": {
              "great": 0,
              "normal": 1,
              "warn": 2,
              "critical": 3
            },
            "env": []
          },
          {
            "job_id": 3,
            "name": "Summary report",
            "prometheus_id": "reporting_hourly_jobs_summary_report",
            "cmd": "/tmp/check /tmp",
            "time_pattern": "yyyy-MM-dd-HH",
            "frequency": "hourly",
            "period_check_offset": 1,
            "timezone": {
              "zone_id": "UTC"
            },
            "lookback": 6,
            "alert_levels": {
              "great": 1,
              "normal": 2,
              "warn": 3,
              "critical": 4
            },
            "env": []
          }
        ]
      },
      "status": [
        {
          "job": {
            "job_id": 0,
            "name": "Raw event logs",
            "prometheus_id": "reporting_hourly_jobs_raw_event_logs",
            "cmd": "/tmp/check /tmp",
            "time_pattern": "yyyy-MM-dd-HH",
            "frequency": "hourly",
            "period_check_offset": 1,
            "timezone": {
              "zone_id": "UTC"
            },
            "lookback": 6,
            "alert_levels": {
              "great": 0,
              "normal": 1,
              "warn": 2,
              "critical": 3
            },
            "env": []
          },
          "updated_at": 1595126730945,
          "period_health": [
            {
              "period": "2020-07-18-20",
              "ok": true
            },
            {
              "period": "2020-07-18-21",
              "ok": false
            },
            {
              "period": "2020-07-18-22",
              "ok": true
            },
            {
              "period": "2020-07-18-23",
              "ok": true
            },
            {
              "period": "2020-07-19-00",
              "ok": false
            },
            {
              "period": "2020-07-19-01",
              "ok": false
            }
          ]
        },
        {
          "job": {
            "job_id": 1,
            "name": "Processed logs",
            "prometheus_id": "reporting_hourly_jobs_processed_logs",
            "cmd": "/tmp/check /tmp",
            "time_pattern": "yyyy-MM-dd-HH",
            "frequency": "hourly",
            "period_check_offset": 1,
            "timezone": {
              "zone_id": "UTC"
            },
            "lookback": 6,
            "alert_levels": {
              "great": 1,
              "normal": 2,
              "warn": 3,
              "critical": 4
            },
            "env": []
          },
          "updated_at": 1595126730945,
          "period_health": [
            {
              "period": "2020-07-18-20",
              "ok": true
            },
            {
              "period": "2020-07-18-21",
              "ok": false
            },
            {
              "period": "2020-07-18-22",
              "ok": true
            },
            {
              "period": "2020-07-18-23",
              "ok": true
            },
            {
              "period": "2020-07-19-00",
              "ok": false
            },
            {
              "period": "2020-07-19-01",
              "ok": false
            }
          ]
        },
        {
          "job": {
            "job_id": 2,
            "name": "Summary metrics",
            "prometheus_id": "reporting_hourly_jobs_summary_metrics",
            "cmd": "/tmp/check /tmp",
            "time_pattern": "yyyy-MM-dd-HH",
            "frequency": "hourly",
            "period_check_offset": 1,
            "timezone": {
              "zone_id": "UTC"
            },
            "lookback": 6,
            "alert_levels": {
              "great": 0,
              "normal": 1,
              "warn": 2,
              "critical": 3
            },
            "env": []
          },
          "updated_at": 1595126730945,
          "period_health": [
            {
              "period": "2020-07-18-20",
              "ok": true
            },
            {
              "period": "2020-07-18-21",
              "ok": false
            },
            {
              "period": "2020-07-18-22",
              "ok": true
            },
            {
              "period": "2020-07-18-23",
              "ok": true
            },
            {
              "period": "2020-07-19-00",
              "ok": false
            },
            {
              "period": "2020-07-19-01",
              "ok": false
            }
          ]
        },
        {
          "job": {
            "job_id": 3,
            "name": "Summary report",
            "prometheus_id": "reporting_hourly_jobs_summary_report",
            "cmd": "/tmp/check /tmp",
            "time_pattern": "yyyy-MM-dd-HH",
            "frequency": "hourly",
            "period_check_offset": 1,
            "timezone": {
              "zone_id": "UTC"
            },
            "lookback": 6,
            "alert_levels": {
              "great": 1,
              "normal": 2,
              "warn": 3,
              "critical": 4
            },
            "env": []
          },
          "updated_at": 1595126730945,
          "period_health": [
            {
              "period": "2020-07-18-20",
              "ok": true
            },
            {
              "period": "2020-07-18-21",
              "ok": false
            },
            {
              "period": "2020-07-18-22",
              "ok": true
            },
            {
              "period": "2020-07-18-23",
              "ok": true
            },
            {
              "period": "2020-07-19-00",
              "ok": false
            },
            {
              "period": "2020-07-19-01",
              "ok": false
            }
          ]
        }
      ]
    }
  }
}
