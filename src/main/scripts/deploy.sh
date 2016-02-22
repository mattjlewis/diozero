#!/bin/sh

version=0.3-SNAPSHOT
pigpioj_version=1.0.0
pi_user=pi
pi_host=sheldon.local
install_folder=/home/pi/diozero

echo "Deploying version ${version} to ${pi_user}@${pi_host}:${install_folder}"

files="../pigpioj/pigpioj-java/target/pigpioj-java-${pigpioj_version}.jar \
	src/main/scripts/runSampleApps.sh \
	diozero-core/target/diozero-core-${version}.jar \
	diozero-imu-devices/target/diozero-imu-devices-${version}.jar \
	diozero-imu-sampleapp/target/diozero-imu-sampleapp-${version}.jar \
	diozero-provider-jdkdio10/target/diozero-provider-jdkdio10-${version}.jar \
	diozero-provider-jdkdio11/target/diozero-provider-jdkdio11-${version}.jar \
	diozero-provider-pi4j/target/diozero-provider-pi4j-${version}.jar \
	diozero-provider-pigpio/target/diozero-provider-pigpio-${version}.jar \
	diozero-provider-wiringpi/target/diozero-provider-wiringpi-${version}.jar \
	diozero-ws281x-java/target/diozero-ws281x-java-${version}.jar"

scp $files ${pi_user}@${pi_host}:${install_folder}
