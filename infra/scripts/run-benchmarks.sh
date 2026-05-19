#!/bin/bash
set -e
echo "Building..."
mvn compile -q
echo "Running benchmarks..."
java -cp target/classes com.cascade.demo.ConcurrencyBenchmark
