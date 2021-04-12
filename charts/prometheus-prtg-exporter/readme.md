# prometheus-prtg-exporter

Prometheus exporter for PRTG.

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| affinity | object | `{}` |  |
| fullnameOverride | string | `""` |  |
| image.pullPolicy | string | `"IfNotPresent"` |  |
| image.repository | string | `"soarinferret/prometheus-prtg-exporter"` |  |
| image.tag | string | `""` | Defined in the `Chart.yaml` |
| imagePullSecrets | list | `[]` |  |
| config | string | See `values.yaml` | This is the yaml config provided to prtg-exporter |
| nameOverride | string | `""` |  |
| nodeSelector | object | `{}` |  |
| podAnnotations | object | `{}` |  |
| podSecurityContext | object | `{}` |  |
| replicaCount | int | `1` |  |
| resources | object | `{}` |  |
| securityContext | object | `{}` |  |
| service.port | int | `9469` |  |
| service.type | string | `"ClusterIP"` |  |
| serviceMonitor.additionalLabels | object | `{}` |  |
| serviceMonitor.enabled | bool | `false` |  |
| serviceMonitor.interval | string | `"1m"` |  |
| serviceMonitor.scrapeTimeout | string | `"1m"` |  |
| tolerations | list | `[]` |  |