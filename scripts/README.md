# OpenSearch CLI Documentation

## Overview

`osctl` is a command-line tool for managing OpenSearch operations.

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

- **create-index**: Creates the specified index.
  [Learn about index creation](https://docs.opensearch.org/docs/latest/api-reference/index-apis/create-index/)
- **delete-index**: Deletes the specified index.
- **update-template**: Updates the specified index template or creates it if it doesn't exist.
  [Learn about index templates](https://opensearch.org/docs/latest/im-plugin/index-templates/)
- **delete-template**: Deletes the specified index template.
- **update-alias**: Creates or updates aliases by using the provided actions.
  [Learn about index aliases](https://opensearch.org/docs/latest/im-plugin/index-alias/)
- **delete-alias**: Removes an alias from the specified index.
- **force-rollover**: Forces an immediate rollover of the specified index.
  [Learn about rollover actions](https://opensearch.org/docs/latest/im-plugin/ism/policies/#rollover-action)
- **create-ism-policy**: Creates the specified ISM policy.
  [Learn about ISM policies](https://opensearch.org/docs/latest/im-plugin/ism/policies/)
- **get-ism-seq**: Retrieves the `seq_no` and `primary_term` of the specified ISM policy.
- **update-ism-policy**: Updates the specified ISM policy.
- **auto-update-ism-policy**: Automatically retrieves `seq_no` and `primary_term`, then updates the
  specified ISM policy.
- **delete-ism-policy**: Deletes the specified ISM policy.

## Guidelines

### Updating an alias

Alias updates are handled by passing a JSON payload to the OpenSearch _aliases API. This payload
can include multiple add and/or remove actions. It's your responsibility to define the correct
actions according to your use case.

Example:
```bash
osctl update-alias -h http://localhost:9200 -d '{
  "actions": [
    { "remove": { "index": "events-v1", "alias": "user_events" }},
    { "add":    { "index": "events-v2", "alias": "user_events" }}
  ]
}'
```

Refer to the official OpenSearch documentation for details:
[Add or remove indexes from an alias](https://docs.opensearch.org/docs/latest/im-plugin/index-alias/#add-or-remove-indexes)

### Updating an index template

When you update an index template in OpenSearch, changes are only applied to new indices. Existing
indices will continue using the old configuration. Depending on your use case, you can choose
between a deferred or immediate application of the changes.

#### If you can wait for the next rollover:

This is the simplest and most common approach for non-urgent changes.

1. Run:
   ```bash
   osctl update-template -h <host> -t <template_name> -d '{...}'
   ```
2. The updated template will be applied automatically the next time OpenSearch performs a rollover
   and creates a new index.

Recommended for: Routine updates that do not require immediate application (e.g., adding new
optional fields, adjusting settings that are not critical to current data).

#### If you need the change to take effect immediately:

For cases where changes must be reflected without delay, follow this process:

```bash
# 1. Update the template
osctl update-template -h <host> -t <template_name> -d '{...}'

# 2. Trigger a rollover
osctl force-rollover -h <host> -i <index_name>

# 3. Update the alias if necessary
osctl update-alias -h <host> -d '{...}'
```

This sequence ensures that the new template is used right away by creating a fresh index and
updating the alias to target it.

Recommended for: Situations where changes must be applied without waiting for the scheduled
rollover.

### Updating an ISM Policy

To update the `events_policy` ISM policy in OpenSearch, you need the current `seq_no` and
`primary_term`. This ensures that the update is performed safely and correctly.

There are two ways to do this:

#### (Recommended) Automatic Update

This method uses the `auto-update-ism-policy` script, which retrieves the `seq_no` and
`primary_term` automatically before applying the update.

Usage:
```bash
osctl auto-update-ism-policy -h <host> -p <policy_name> -d '{...}'
```

This script will:

1. Retrieve the current `seq_no` and `primary_term` of the `events_policy`.
2. Call `update-ism-policy` with the correct values to apply the update.

If anything goes wrong, you can fall back to the manual method below.

#### Manual Update

If the automatic method fails, or you want full control, you can retrieve the metadata manually and
then run the update yourself.

##### Step 1: Retrieve `seq_no` and `primary_term`

Use the helper script `get-ism-seq`:

```bash
osctl get-ism-seq -h <opensearch_host> -p <policy_name>
```

You will get an output like:

```
seq_no=42
primary_term=3
```

##### Step 2: Apply the update using `update-ism-policy`

Run the update script using the retrieved values:

```bash
osctl update-ism-policy -h <opensearch_host> -p <policy_name> -d '<policy_json>' -s <seq_no> -r <primary_term>
```
