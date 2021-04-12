# prtg_exporter
Prometheus exporter for PRTG sensor metrics, written in Kotlin.

## Build

NOTE: Tests are disabled during docker build since we do not have access to a public PRTG server

```bash
docker build -t prtg-exporter:latest .
```

## Configuration

Create a `application-local.yml` file with the following:

```yaml
server:
  port: 8090
prtg:
  url: https://prtg.example.com
  username: myuser
  passhash: 1234567890
  sensors:
    filter:
      enabled: true
      tags: pingsensor 

management:
  security:
    enabled: false
```

## Run

Using the previous configuration:

```bash
docker run -it --rm -v ${PWD}/application-local.yml:/prtg/application.yml -p 8090:8090  prtg-exporter:latest
```

## Usage

Below is an example scrape config to use with Prometheus:

```
scrape_configs:
  - job_name: prtg_job
    metrics_path: /prometheus
    scrape_interval: 1m
    static_configs:
      - targets:
        - 127.0.0.1:8090
```

## Attribution

Special thanks to:

* [mstipanov](https://github.com/mstipanov/prtg-exporter) - Original author of this exporter
* [rees](https://stackoverflow.com/a/52244937) - Provided a library via a stack overflow answer, licensed separately under the CC-BY-SA