# OpenSearch CLI Documentation

## Overview

`osctl` is a command-line tool for managing OpenSearch operations for Pillarbox monitoring.

## Installation

To install `osctl`, follow these steps to set up a dedicated installation directory and run the
install script. The `install-osctl.sh` script will:

- Download the specified version from GitHub
- Install it in a versioned folder (`<install_dir>/<version>/`)
- Create or update the `current` symlink to always point to the active version

### Step-by-Step Guide

1. **Create a dedicated osctl directory**

   We recommend installing `osctl` in a hidden folder in your home directory for a clean setup. Open
   your terminal and run:

   ```bash
   mkdir -p ~/.osctl
   cd ~/.osctl
   ```

2. **Download the installation script**

   Download the [`install-osctl.sh`](./install-osctl.sh) script from the repo:

   ```bash
   wget https://raw.githubusercontent.com/SRGSSR/pillarbox-monitoring-transfer/scripts/install-osctl.sh
   chmod +x install-osctl.sh
   ```

3. **Run the installer for your desired version**

   Replace `4.1.0` with the version you want. The script will download the asset from GitHub,
   install it into a versioned folder (e.g., `~/.osctl/4.1.0/`), and update the `current` symlink
   accordingly:

   ```bash
   ./install-osctl.sh -v 4.1.0
   ```

4. **Add osctl to your PATH (optional)**

   To easily run `osctl` from anywhere, add the following line to your shell configuration file (
   e.g., `~/.bashrc` or `~/.zshrc`):

   ```bash
   export PATH="$HOME/.osctl/current:$PATH"
   ```

   Then reload your shell:

   ```bash
   source ~/.bashrc   # or source ~/.zshrc
   ```

5. **Run osctl**

   Now you can run:

   ```bash
   osctl <command> [options]
   ```

## Usage

```bash
osctl <command> [options]
```

### Commands

- **update-template**: Updates the `events_template` index template.
  [Learn about index templates](http://opensearch.org/docs/latest/im-plugin/index-templates/)

- **update-alias**: Updates the `user_events` alias with a new configuration.
  [Learn about index aliases](https://opensearch.org/docs/latest/im-plugin/index-alias/)

- **force-rollover**: Forces an immediate rollover of the `events` index.
  [Learn about rollover actions](https://opensearch.org/docs/latest/im-plugin/ism/policies/#rollover-action)

## Examples

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
