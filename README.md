# flink-state-processor-example
An example of using Flink's State Processor API to read KafkaSource state.

## Setup
* You need `jdk` and maven installed on your system to build the project.

## Build

```
mvn clean package
```

## Run

To run the FlinkTest, see https://nightlies.apache.org/flink/flink-docs-stable/docs/try-flink/local_installation/

```
./bin/flink run -c FlinkTest target/flink-state-processor-example-1.0-SNAPSHOT-jar-with-dependencies.jar 
```

To run the StateProcessorTest (note that an example checkpoint can be found in `resources`)

```
java -cp target/flink-state-processor-example-1.0-SNAPSHOT-jar-with-dependencies.jar StateProcessorTest
```
