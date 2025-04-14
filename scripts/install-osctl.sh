#!/bin/bash

ASSET_NAME="osctl.zip"
REPO="SRGSSR/pillarbox-monitoring-transfer"

usage() {
  echo ""
  echo "This script updates the osctl tools to the specified version from GitHub releases."
  echo ""
  echo "Options:"
  echo "  -v <version>   The version to install (e.g., 1.2.3)"
  echo ""
  echo "Example:"
  echo "  $0 -v 1.2.3"
  echo ""
  echo "This script ensures that the latest osctl tools are installed and replaces any existing files in the current directory."
  exit 1
}

while getopts "v:" opt; do
  case ${opt} in
    v ) VERSION=$OPTARG ;;
    * ) usage ;;
  esac
done

if [ -z "$VERSION" ]; then
  usage
fi

INSTALL_BASE="$(cd "$(dirname "$0")" && pwd)"
VERSION_DIR="${INSTALL_BASE}/${VERSION}"
CURRENT_SYMLINK="${INSTALL_BASE}/current"
TMP_DIR="$(mktemp -d)"
ZIP_PATH="${TMP_DIR}/${ASSET_NAME}"
DOWNLOAD_URL="https://github.com/${REPO}/releases/download/v${VERSION}/${ASSET_NAME}"

echo "Installing osctl version ${VERSION}"
echo "Installation base: ${INSTALL_BASE}"
echo "Download URL: ${DOWNLOAD_URL}"

if [ -d "$VERSION_DIR" ]; then
  echo "Version ${VERSION} already installed at ${VERSION_DIR}"
else
  echo "Downloading ZIP..."
  DOWNLOAD_RESPONSE=$(curl -sSL -w "%{http_code}" -o "$ZIP_PATH" "$DOWNLOAD_URL")
  CURL_EXIT_CODE=$?

  if [ $CURL_EXIT_CODE -ne 0 ] || [ "${DOWNLOAD_RESPONSE: -3}" -ge 400 ]; then
    echo "Download failed. HTTP status: ${DOWNLOAD_RESPONSE: -3}. Does v${VERSION} exist?"
    rm -rf "$TMP_DIR"
    exit 1
  fi
  echo "Download complete."

  echo "Extracting ZIP..."
  if ! unzip -q "$ZIP_PATH" -d "$TMP_DIR/unpacked"; then
    echo "Unzip failed."
    rm -rf "$TMP_DIR"
    exit 1
  fi
  echo "Extraction complete."

  mkdir -p "$VERSION_DIR"
  cp -r "$TMP_DIR/unpacked/"* "$VERSION_DIR"
  echo "Files copied to ${VERSION_DIR}"
fi

echo "Updating 'current' symlink..."
rm -f "$CURRENT_SYMLINK"
ln -s "$VERSION_DIR" "$CURRENT_SYMLINK"
echo "Symlink updated: ${CURRENT_SYMLINK} -> ${VERSION_DIR}"

rm -rf "$TMP_DIR"
echo "Install complete. osctl ${VERSION} is now active."
