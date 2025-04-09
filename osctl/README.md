# OpenSearch CLI Documentation

## Overview

`osctl` is a command-line tool for managing OpenSearch operations for pillarbox monitoring.

## Usage

```bash
./osctl <command> [options]
```

### Commands:

- **update-template**: Updates the `events_template` index template.
  [Learn about index templates](http://opensearch.org/docs/latest/im-plugin/index-templates/)
- **update-alias**: Updates the `user_events` alias with a new configuration.
  [Learn about index aliases](https://opensearch.org/docs/latest/im-plugin/index-alias/)
- **force-rollover**: Forces an immediate rollover of the `events` index.
  [Learn about rollove actions](https://opensearch.org/docs/latest/im-plugin/ism/policies/#rollover-action)
- update: Updates the osctl tools to the specified version from GitHub releases.

### Examples:

#### Update the index template:

```bash
osctl update-template -h http://localhost:9200 -t '{...}'
```

#### Update an alias:

```bash
osctl update-alias -h http://localhost:9200 -t '{...}'
```

#### Force an immediate rollover:

```bash
osctl force-rollover -h http://localhost:9200
```

#### Update osctl tools to a specific version:

```bash
osctl update -v 1.2.3
```

## Guidelines for Updating a Template

### If you can wait for the next rollover:

1. Run:
   ```bash
   osctl update-template -h <opensearch_host> -t '{...}'
   ```
2. The changes will take effect on the next rollover automatically.

### If you need the change to take effect immediately:

1. Update the template:
   ```bash
   osctl update-template -h <opensearch_host> -t '{...}'
   ```
2. Force a rollover:
   ```bash
   osctl force-rollover -h <opensearch_host>
   ```
3. Update the alias:
   ```bash
   osctl update-alias -h <opensearch_host> -t '{...}'
   ```

This ensures that the new template is applied right away instead of waiting for the next automatic
rollover.
