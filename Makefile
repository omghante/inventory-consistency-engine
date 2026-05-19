.PHONY: build test benchmark demo-flash demo-system demo-failure demo-all clean

build:
	mvn compile -q

test:
	mvn test

benchmark: build
	java -cp target/classes com.cascade.demo.ConcurrencyBenchmark

demo-flash: build
	java -cp target/classes com.cascade.demo.PS5FlashSaleDemo

demo-system: build
	java -cp target/classes com.cascade.demo.FullSystemSimulation

demo-failure: build
	java -cp target/classes com.cascade.demo.FailureRecoveryDemo

demo-all: demo-flash demo-system demo-failure benchmark

clean:
	mvn clean -q
