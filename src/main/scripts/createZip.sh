#!/bin/sh

pigpioj_version=1.0.0
version=0.3-SNAPSHOT

files="../pigpioj/pigpioj-java/target/pigpioj-java-${pigpioj_version}.jar \
	$USERPROFILE/.m2/repository/org/tinylog/tinylog/1.0.3/tinylog-1.0.3.jar \
	diozero-core/target/diozero-core-${version}.jar \
	diozero-imu-devices/target/diozero-imu-devices-${version}.jar \
	diozero-imu-sampleapp/target/diozero-imu-sampleapp-${version}.jar \
	diozero-imu-visualiser/target/diozero-imu-visualiser-${version}.jar \
	diozero-provider-jdkdio10/target/diozero-provider-jdkdio10-${version}.jar \
	diozero-provider-jdkdio11/target/diozero-provider-jdkdio11-${version}.jar \
	diozero-provider-pi4j/target/diozero-provider-pi4j-${version}.jar \
	diozero-provider-pigpio/target/diozero-provider-pigpio-${version}.jar \
	diozero-provider-wiringpi/target/diozero-provider-wiringpi-${version}.jar \
	diozero-ws281x-java/target/diozero-ws281x-java-${version}.jar"


rm -f diozero-${version}.zip
zip -j target/diozero-${version}.zip $files
