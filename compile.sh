#!/usr/bin/env bash
set -e

export MMCOE_DB_PASSWORD="${MMCOE_DB_PASSWORD:-12345}"

mkdir -p build/common build/dsa build/server build/gateway build/tools

echo "[1/5] Compiling common models & security..."
javac -d build/common common/src/model/*.java common/src/security/*.java

echo "[2/5] Compiling DSA layer..."
javac -cp build/common -d build/dsa \
  dsa/src/datastructure/book/*.java \
  dsa/src/datastructure/user/*.java \
  dsa/src/datastructure/issue/*.java \
  dsa/src/datastructure/fine/*.java \
  dsa/src/kpi/*.java

echo "[3/5] Compiling TCP server and database layer..."
javac -cp "build/common:build/dsa:lib/mysql-connector-j-26.7.0.jar" -d build/server \
  database/src/*.java \
  tcp-server/src/server/*.java

echo "[4/5] Compiling HTTP gateway..."
javac -cp "build/common:build/dsa:build/server:lib/mysql-connector-j-26.7.0.jar" -d build/gateway \
  gateway/src/gateway/*.java

echo "[5/5] Compiling test & benchmark tools..."
javac -cp "build/common:build/dsa:build/server:lib/mysql-connector-j-26.7.0.jar" -d build/tools \
  tools/*.java

echo ""
echo "BUILD SUCCESSFUL."
