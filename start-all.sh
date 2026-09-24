#!/usr/bin/env bash
set -e

export MMCOE_DB_USER="${MMCOE_DB_USER:-root}"
export MMCOE_DB_PASSWORD="${MMCOE_DB_PASSWORD:-12345}"

# Kill any existing server instances using ports 9000 or 8080
pkill -f "server.TCPServer" 2>/dev/null || true
pkill -f "gateway.HTTPGateway" 2>/dev/null || true
fuser -k 9000/tcp 8080/tcp 2>/dev/null || true
sleep 1

if [ ! -f build/server/server/TCPServer.class ] || [ ! -f build/gateway/gateway/HTTPGateway.class ]; then
  ./compile.sh
fi

echo "Starting TCP Server on port 9000..."
./start-tcp-server.sh &
TCP_PID=$!

sleep 2

echo "Starting HTTP Gateway on port 8080..."
./start-gateway.sh &
GW_PID=$!

sleep 2

echo "Application started!"
echo "HTTP Gateway: http://localhost:8080/landing.html"
echo "Press Ctrl+C to stop servers."

trap "kill $TCP_PID $GW_PID 2>/dev/null" EXIT INT TERM

wait $TCP_PID $GW_PID
