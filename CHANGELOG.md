## [2.0.1](https://github.com/SRGSSR/pillarbox-monitoring-transfer/compare/v2.0.0...v2.0.1) (2024-10-10)


### Enhancements and Bug Fixes 🐛

* disable ktlint for the docker build step ([6d1a467](https://github.com/SRGSSR/pillarbox-monitoring-transfer/commit/6d1a467c5a62f924ae5dfef7c2a117adfca21b37))

## [2.0.0](https://github.com/SRGSSR/pillarbox-monitoring-transfer/compare/v1.0.0...v2.0.0) (2024-10-10)


### ⚠ BREAKING CHANGES

* Updated all package names and configuration from `pillarbox.qos` to
`pillarbox.monitoring`. This includes changes to environment variables, which now use the
prefix `PILLARBOX_MONITORING_*` instead of `PILLARBOX_QOS_*`. Ensure that all relevant
configurations are updated to reflect this change when deploying or running the service.

### Refactor 🔩

* change qos naming to monitoring ([f50a26c](https://github.com/SRGSSR/pillarbox-monitoring-transfer/commit/f50a26cb9c97179553956e362cdafdf2a6aeacc8))

## 1.0.0 (2024-10-09)


### New Features 🚀

* initial setup for pillarbox-monitoring data transfer service ([62da9ba](https://github.com/SRGSSR/pillarbox-monitoring-transfer/commit/62da9ba7821e8f5a0e3a28e102f304e16fcfeb98))


### Chore 🧹

* setup project ([6a3e556](https://github.com/SRGSSR/pillarbox-monitoring-transfer/commit/6a3e556f9f262e519492c48daffce8d0d9a02aaf))
