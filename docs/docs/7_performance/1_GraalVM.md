---
parent: Performance
nav_order: 1
permalink: /performance/graalvm.html
title: GraalVM
---

# GraalVM

{: .note }
> 64-bit only!

Note I initially tried doing this on a Pi4 with 2GB RAM - everything worked ok until atempting to
include the resource configuration JSON file. I got the following 137 error from the GraalVM native
image builder (out of memory) - `Error: Image build request failed with exit status 137`.
Switching to a Pi4 with 8GB RAM and the native image command completed successfully for the
`GpioPerfTest` sample application.

I haven't attempted to cross-compile via a qemu image running on a desktop / laptop.

## Install GraalVM into /usr/lib/jvm

```shell
wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-21.0.0.2/graalvm-ce-java11-linux-aarch64-21.0.0.2.tar.gz
tar zxf graalvm-ce-java11-linux-aarch64-21.0.0.2.tar.gz
sudo mv graalvm-ce-java11-21.0.0.2 /usr/lib/jvm/.
```

Edit `.zshrc`:
```shell
export GRAALVM_HOME=/usr/lib/jvm/graalvm-ce-java11-21.0.0.2
export PATH=${PATH}:${GRAALVM_HOME}/bin
```

Install native-image:
```shell
gu install native-image
```

## Test

```shell
cat <<EOF > HelloWorld.java
public class HelloWorld {
  public static void main(String[] args) {
    System.out.println("Hello, World!");
  }
}
EOF

javac HelloWorld.java
native-image HelloWorld
./helloworld
```

## Extract Config Files for an Application

```shell
$GRAALVM_HOME/bin/java -agentlib:native-image-agent=config-output-dir=config -cp diozero-sampleapps-{{site.version}}.jar:diozero-core-|{{site.version}}.jar:tinylog-api-2.2.1.jar:tinylog-impl-2.2.1.jar com.diozero.sampleapps.perf.GpioPerfTest
```

## Example - GpioPerfTest

```shell
native-image -H:JNIConfigurationFiles=./config/jni-config.json -H:ReflectionConfigurationFiles=./config/reflect-config.json -H:ResourceConfigurationFiles=./config/resource-config.json --allow-incomplete-classpath --no-fallback -H:+TraceServiceLoaderFeature -H:+ReportExceptionStackTraces -cp diozero-sampleapps-{{site.version}}.jar:diozero-core-{{site.version}}.jar:tinylog-api-2.2.1.jar:tinylog-impl-2.2.1.jar com.diozero.sampleapps.perf.GpioPerfTest
```

## Enabling the GraalVM JVMCICompiler

Run with `-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler`, for example:
```shell
java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler -cp diozero-sampleapps-1.2.0.jar com.diozero.sampleapps.perf.GpioPerfTest 21 50000000
```

## Results

Default behaviour:
```shell
> java -cp diozero-sampleapps-1.2.0.jar com.diozero.sampleapps.perf.GpioPerfTest 21 50000000
19:59:21.284 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.main - Starting GPIO performance test using GPIO 21 with 50000000 iterations
19:59:23.387 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 50,000,000 iterations: 1.847 s, frequency: 27,070,926 Hz
19:59:25.224 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 50,000,000 iterations: 1.828 s, frequency: 27,352,298 Hz
19:59:27.024 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 50,000,000 iterations: 1.799 s, frequency: 27,793,218 Hz
19:59:28.823 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 50,000,000 iterations: 1.797 s, frequency: 27,824,151 Hz
19:59:30.623 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 50,000,000 iterations: 1.798 s, frequency: 27,808,676 Hz
```

With a smaller number of iterations you see the impact of the JVM JIT compiler in the early results:
```shell
> java -cp diozero-sampleapps-1.2.0.jar com.diozero.sampleapps.perf.GpioPerfTest 21 500000
19:50:27.109 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.main - Starting GPIO performance test using GPIO 21 with 500000 iterations
19:50:27.417 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 500,000 iterations: 0.053 s, frequency: 9,433,962 Hz
19:50:27.468 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 500,000 iterations: 0.043 s, frequency: 11,627,907 Hz
19:50:27.491 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 500,000 iterations: 0.020 s, frequency: 25,000,000 Hz
19:50:27.510 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 500,000 iterations: 0.018 s, frequency: 27,777,778 Hz
19:50:27.530 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 500,000 iterations: 0.018 s, frequency: 27,777,778 Hz
```

With the GraalVM JVMCICompiler the speed really ramps up (I haven't verified the frequency other than by using an LED):
```shell
> java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler -cp diozero-sampleapps-1.2.0.jar com.diozero.sampleapps.perf.GpioPerfTest 21 50000000
19:56:47.368 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.main - Starting GPIO performance test using GPIO 21 with 50000000 iterations
19:56:51.144 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 50,000,000 iterations: 3.488 s, frequency: 14,334,862 Hz
19:56:54.627 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 50,000,000 iterations: 3.469 s, frequency: 14,413,376 Hz
19:56:55.925 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 50,000,000 iterations: 1.296 s, frequency: 38,580,247 Hz
19:56:56.434 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 50,000,000 iterations: 0.498 s, frequency: 100,401,606 Hz
19:56:56.918 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 50,000,000 iterations: 0.480 s, frequency: 104,166,667 Hz
```

With a smaller number of iterations you see that the GraalVM JVMCICompiler doesn't get going:
```shell
> java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler -cp diozero-sampleapps-1.2.0.jar com.diozero.sampleapps.perf.GpioPerfTest 21 500000
10:57:14.146 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.main - Starting GPIO performance test using GPIO 21 with 500000 iterations
10:57:14.858 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 500,000 iterations: 0.112 s, frequency: 4,464,286 Hz
10:57:14.979 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 500,000 iterations: 0.101 s, frequency: 4,950,495 Hz
10:57:15.080 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 500,000 iterations: 0.100 s, frequency: 5,000,000 Hz
10:57:15.182 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 500,000 iterations: 0.100 s, frequency: 5,000,000 Hz
10:57:15.285 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 500,000 iterations: 0.100 s, frequency: 5,000,000 Hz
```

With the GraalVM native image:
```shell
> ./com.diozero.sampleapps.perf.gpioperftest 21 10000000
10:45:57.735 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.main - Starting GPIO performance test using GPIO 21 with 10000000 iterations
10:45:58.912 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 10,000,000 iterations: 1.162 s, frequency: 8,605,852 Hz
10:46:00.115 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 10,000,000 iterations: 1.203 s, frequency: 8,312,552 Hz
10:46:01.278 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 10,000,000 iterations: 1.163 s, frequency: 8,598,452 Hz
10:46:02.441 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 10,000,000 iterations: 1.163 s, frequency: 8,598,452 Hz
10:46:03.604 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 10,000,000 iterations: 1.163 s, frequency: 8,598,452 Hz
```

Native image and smaller number of iterations:
```shell
> ./com.diozero.sampleapps.perf.gpioperftest 21 1000000
11:00:36.962 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.main - Starting GPIO performance test using GPIO 21 with 1000000 iterations
11:00:37.132 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 1,000,000 iterations: 0.153 s, frequency: 6,535,948 Hz
11:00:37.249 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 1,000,000 iterations: 0.117 s, frequency: 8,547,009 Hz
11:00:37.365 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 1,000,000 iterations: 0.116 s, frequency: 8,620,690 Hz
11:00:37.481 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 1,000,000 iterations: 0.116 s, frequency: 8,620,690 Hz
11:00:37.597 [main] INFO com.diozero.sampleapps.perf.GpioPerfTest.test - Duration for 1,000,000 iterations: 0.116 s, frequency: 8,620,690 Hz
```

The results are interesting but mainly as you would expect:

* Using the JVM JIT provides the best performance eventually but takes its time to get "warmed up".
* The new experimental GraalVM JVMCICompiler in JDK11 appears to bring significant performance benefits, but needs to be verified.
* GraalVM compiled native images provide slower overall performance but is much more consistent.
