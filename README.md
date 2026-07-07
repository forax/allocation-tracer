# Allocation Tracer

A small Java Instrumentation Agent that records allocation sites at runtime.

The agent uses ASM to instrument application classes and logs every object and array allocation
together with its allocation site (class, method, descriptor and line number).

## Features

* Records object allocations (`new`)
* Records primitive and object array allocations
* Records multidimensional array allocations
* Uses a bootstrap-loaded probe so instrumented classes can safely call into the agent

## Building

Requirements:

* JDK 11 or newer
* Maven

Build the agent:

```bash
mvn package
```

The resulting agent JAR is in the folder `target`:

```text
target/allocation-agent.jar
```

## Running an application with the agent

Attach the agent when starting a JVM using the `-javaagent` option.

```bash
java -javaagent:/path/to/allocation-agent.jar -jar my-application.jar
```

or when running a class directly:

```bash
java -javaagent:/path/to/allocation-agent.jar -cp myapp.jar com.example.Main
```

By default, the agent writes the allocation log to:

```text
allocations.log
```

You can specify another output file by passing it as the agent argument:

```bash
java -javaagent:/path/to/allocation-agent.jar=mylog.txt \
     -jar my-application.jar
```

## Log format

Each line corresponds to one allocation.

Object allocations:

```text
counter:class:method(descriptor):line:allocated-type
```

Example:

```text
42:com.example.Main:run()V:37:java/lang/String
```

Array allocations:

```text
counter:class:method(descriptor):line:size
```

where `size` is the shallow object size reported by `Instrumentation.getObjectSize()`.

## Tracing any Java program

The agent can be used with almost any Java application.

Examples:

Run a JAR:

```bash
java -javaagent:/path/to/allocation-agent.jar -jar application.jar
```

Run a Maven application:

```bash
mvn exec:java \
    -Dexec.jvmArgs="-javaagent:/path/to/allocation-agent.jar"
```

Run a Gradle application:

```bash
./gradlew run \
    --args="" \
    -Dorg.gradle.jvmargs="-javaagent:/path/to/allocation-agent.jar"
```

Run unit tests:

```bash
mvn test \
    -DargLine="-javaagent:/path/to/allocation-agent.jar"
```

or

```bash
./gradlew test \
    -Dorg.gradle.jvmargs="-javaagent:/path/to/allocation-agent.jar"
```

Any JVM started with the `-javaagent` option will produce an allocation log.

## Limitations

* Only explicit allocation bytecodes are instrumented (`new`, `newarray`, `anewarray`, `multianewarray`).
* JDK/platform classes are intentionally skipped.
* While array sizes are reported by `Instrumentation.getObjectSize()`, object size are not.
* The logging mechanism is intentionally simple and optimized for correctness rather than throughput.

