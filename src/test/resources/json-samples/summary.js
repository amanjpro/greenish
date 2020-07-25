module.exports = {
  "testObject": {
    [
      {
        "group_id": 0,
        "name": "Reporting Hourly Jobs",
        "status": [
          {
            "job_id": 0,
            "name": "Raw event logs",
            "missing": 3,
            "alert_level": "critical"
          },
          {
            "job_id": 1,
            "name": "Processed logs",
            "missing": 3,
            "alert_level": "warn"
          },
          {
            "job_id": 2,
            "name": "Summary metrics",
            "missing": 3,
            "alert_level": "critical"
          },
          {
            "job_id": 3,
            "name": "Summary report",
            "missing": 3,
            "alert_level": "warn"
          }
        ]
      },
      {
        "group_id": 1,
        "name": "Nighly jobs",
        "status": [
          {
            "job_id": 0,
            "name": "Even log compacter",
            "missing": 3,
            "alert_level": "normal"
          },
          {
            "job_id": 1,
            "name": "Event log archiver",
            "missing": 3,
            "alert_level": "great"
          }
        ]
      },
      {
        "group_id": 2,
        "name": "Monthly jobs",
        "status": [
          {
            "job_id": 0,
            "name": "Billing reports",
            "missing": 1,
            "alert_level": "normal"
          },
          {
            "job_id": 1,
            "name": "Exporter jobs",
            "missing": 1,
            "alert_level": "normal"
          }
        ]
      }
    ]
  }
}
