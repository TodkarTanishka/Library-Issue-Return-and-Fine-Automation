#!/usr/bin/env bash
set -e

export MMCOE_DB_USER="${MMCOE_DB_USER:-root}"
export MMCOE_DB_PASSWORD="${MMCOE_DB_PASSWORD:-12345}"

if [ ! -f build/server/server/TCPServer.class ]; then
  ./compile.sh
fi

java -cp "build/common:build/dsa:build/server:lib/mysql-connector-j-26.7.0.jar" server.TCPServer
