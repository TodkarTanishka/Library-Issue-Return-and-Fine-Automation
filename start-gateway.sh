#!/usr/bin/env bash
set -e

if [ ! -f build/gateway/gateway/HTTPGateway.class ]; then
  ./compile.sh
fi

java -cp "build/common:build/dsa:build/server:build/gateway:lib/mysql-connector-j-26.7.0.jar" gateway.HTTPGateway frontend
