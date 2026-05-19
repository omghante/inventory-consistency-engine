#!/bin/bash
set -e
mvn compile -q
java -cp target/classes com.cascade.demo.FullSystemSimulation
