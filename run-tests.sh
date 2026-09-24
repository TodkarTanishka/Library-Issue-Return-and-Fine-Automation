#!/usr/bin/env bash
set -e

echo "=================================================="
echo "  MMCOE LIBRARY MANAGEMENT SYSTEM - TEST RUNNER"
echo "=================================================="

./compile.sh

echo ""
echo "[TEST SUITE 1/2] Running OS Concurrency Race Condition Test (50 Threads)..."
java -cp "build/tools:build/server:build/dsa:build/common:lib/mysql-connector-j-26.7.0.jar" tools.ConcurrencyRaceTest

echo ""
echo "[TEST SUITE 2/2] Running Time Machine & Background Automation Test..."
java -cp "build/tools:build/server:build/dsa:build/common:lib/mysql-connector-j-26.7.0.jar" tools.AutomationTest

echo ""
echo "=================================================="
echo "  ALL VERIFICATION TESTS PASSED SUCCESSFULLY!"
echo "=================================================="
