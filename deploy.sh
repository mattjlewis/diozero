#!/bin/sh

version=0.3-SNAPSHOT
echo "version=$version"

files="../pigpioj/pigpioj-java/target/pigpioj-java-1.0.0.jar \
	diozero-core/runSampleApps.sh \
	diozero-core/target/diozero-core-$version.jar \
	diozero-imu-sampleapp/target/diozero-imu-sampleapp-$version.jar \
	diozero-provider-jdkdio10/target/diozero-provider-jdkdio10-$version.jar \
	diozero-provider-jdkdio11/target/diozero-provider-jdkdio11-$version.jar \
	diozero-provider-pi4j/target/diozero-provider-pi4j-$version.jar \
	diozero-provider-pigpio/target/diozero-provider-pigpio-$version.jar \
	diozero-provider-wiringpi/target/diozero-provider-wiringpi-$version.jar"

scp $files pi@sheldon.local:/home/pi/diozero
