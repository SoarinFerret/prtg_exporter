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

### Filters

There are 3 different filter options:
* `tags`: Provide an array of tags
* `objids`: Provide an array of Object IDs
* `groups`: Provide an array of group names

Here is an incomplete example of multiple groups and a tag:

```yaml
prtg:
  sensors:
    filter:
      enabled: true
      groups: VMWare, HyperV
      tags: pingsensor 
...
```

### Debug Logs

If you would like to see the URL being generated for PRTG, you can enable debug logs like so:

```yaml
...
logging.level:
  org.infobip.prometheus.prtgexporter.prtg: debug
...
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

### Kubernetes

A helm chart is provided for your convenience. Please see the [chart readme](charts/prometheus-prtg-exporter/readme.md) for more information.

## Attribution

Special thanks to:

* [mstipanov](https://github.com/mstipanov/prtg-exporter) - Original author of this exporter
* [rees](https://stackoverflow.com/a/52244937) - Provided a library via a stack overflow answer, licensed separately under the CC-BY-SA