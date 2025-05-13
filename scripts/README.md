# OpenSearch CLI Documentation

## Overview

`osctl` is a command-line tool for managing OpenSearch operations for Pillarbox monitoring.

## Installation

To install `osctl`, follow these steps to set up a dedicated installation directory and run the
installation script. The `install-osctl.sh` script will:

- Download the specified version from GitHub
- Install it in a versioned folder (`<install_dir>/<version>/`)
- Create or update the `current` symlink to always point to the active version

### Step-by-Step Guide

1. **Install required dependencies**

   Before running the installation script, make sure you have `jq` and `curl` installed. These tools
   are required to download and process data during installation.

   * On **Debian/Ubuntu** and other apt-based systems:
     ```bash
     sudo apt update
     sudo apt install -y curl jq
     ```

   * Or in a yum-based systems:
     ```bash
     sudo yum install -y curl jq
     ```

2. **Create a dedicated osctl directory**

   We recommend installing `osctl` in a hidden folder in your home directory for a clean setup. Open
   your terminal and run:

   ```bash
   mkdir -p ~/.osctl
   cd ~/.osctl
   ```

3. **Download the installation script**

   Download the [`install-osctl.sh`](./install-osctl.sh) script from the repo:

   ```bash
   wget https://raw.githubusercontent.com/SRGSSR/pillarbox-monitoring-transfer/refs/heads/main/scripts/install-osctl.sh
   chmod +x install-osctl.sh
   ```

4. **Run the installer for your desired version**

   Replace `4.1.0` with the version you want. The script will download the asset from GitHub,
   install it into a versioned folder (e.g., `~/.osctl/4.1.0/`), and update the `current` symlink
   accordingly:

   ```bash
   ./install-osctl.sh -v 4.1.0
   ```

5. **Add osctl to your PATH (optional)**

   To easily run `osctl` from anywhere, add the following line to your shell configuration file (
   e.g., `~/.bashrc` or `~/.zshrc`):

   ```bash
   export PATH="$HOME/.osctl/current:$PATH"
   ```

   Then reload your shell:

   ```bash
   source ~/.bashrc   # or source ~/.zshrc
   ```

6. **Run osctl**

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
- **get-ism-seq**: Retrieves the current `seq_no` and `primary_term` of the `events_policy` ISM
  policy.
- **update-ism-policy**: Updates the `events_policy` ISM policy in OpenSearch.
- **auto-update-ism-policy**: Automatically retrieves `seq_no` and `primary_term`, then updates the
  `events_policy` ISM policy.
  [Learn about ISM policies](https://docs.opensearch.org/docs/latest/im-plugin/ism/policies/)

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

#### Update ISM Policy

```bash
osctl auto-update-ism-policy -h http://localhost:9200 -d '{...}'
```

## Guidelines for Updating the index template

When you update an index template in OpenSearch, changes are only applied to new indices. Existing
indices will continue using the old configuration. Depending on your use case, you can choose
between a deferred or immediate application of the changes.

### If you can wait for the next rollover:

This is the simplest and most common approach for non-urgent changes.

1. Run:
   ```bash
   osctl update-template -h <opensearch_host> -t '{...}'
   ```
2. The updated template will be applied automatically the next time OpenSearch performs a rollover
   and creates a new index.

Recommended for: Routine updates that do not require immediate application (e.g., adding new
optional fields, adjusting settings that are not critical to current data).

### If you need the change to take effect immediately:

For cases where changes must be reflected without delay, follow this process:

1. Update the template:
   ```bash
   osctl update-template -h <opensearch_host> -t '{...}'
   ```
2. Trigger an immediate rollover:
   ```bash
   osctl force-rollover -h <opensearch_host>
   ```
3. Update the alias to point to the new index:
   ```bash
   osctl update-alias -h <opensearch_host> -t '{...}'
   ```

This sequence ensures that the new template is used right away by creating a fresh index and
updating the alias to target it.

Recommended for: Situations where changes must be applied without waiting for the scheduled
rollover.

## Guidelines for Updating the ISM Policy

To update the `events_policy` ISM policy in OpenSearch, you need the current `seq_no` and
`primary_term`. This ensures that the update is performed safely and correctly.

There are two ways to do this:

### 🔁 **Recommended: Automatic Update**

This method uses the `auto-update-ism-policy` script, which retrieves the `seq_no` and
`primary_term` automatically before applying the update.

#### Usage

```bash
osctl auto-update-ism-policy -h <opensearch_host> -d '<policy_json>'
```

Replace `<opensearch_host>` with your OpenSearch URL (e.g., `http://localhost:9200`) and
`<policy_json>` with your ISM policy definition.

#### Example

```bash
osctl auto-update-ism-policy -h http://localhost:9200 -d '{...}'
```

This script will:

1. Retrieve the current `seq_no` and `primary_term` of the `events_policy`.
2. Call `update-ism-policy` with the correct values to apply the update.

If anything goes wrong, you can fall back to the manual method below.

### 🛠️ **Manual Update**

If the automatic method fails, or you want full control, you can retrieve the metadata manually and
then run the update yourself.

#### Step 1: Retrieve `seq_no` and `primary_term`

Use the helper script `get-ism-seq`:

```bash
osctl get-ism-seq -h <opensearch_host>
```

You will get an output like:

```
seq_no=42
primary_term=3
```

#### Step 2: Apply the update using `update-ism-policy`

Run the update script using the retrieved values:

```bash
osctl update-ism-policy -h <opensearch_host> -d '<policy_json>' -s <seq_no> -r <primary_term>
```

#### Example

```bash
osctl update-ism-policy -h http://localhost:9200 -d '{...}' -s 42 -r 3
```
