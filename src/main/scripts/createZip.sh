###
# #%L
# Organisation: mattjlewis
# Project:      Device I/O Zero
# Filename:     createZip.sh  
# 
# This file is part of the diozero project. More information about this project
# can be found at http://www.diozero.com/
# %%
# Copyright (C) 2016 - 2017 mattjlewis
# %%
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
# #L%
###
#!/bin/sh

version=0.11-SNAPSHOT
pigpioj_version=2.1

files="../pigpioj/pigpioj-java/target/pigpioj-java-${pigpioj_version}.jar \
	$USERPROFILE/.m2/repository/org/tinylog/tinylog/1.2/tinylog-1.2.jar \
	diozero-core/target/diozero-core-${version}.jar \
	diozero-imu-devices/target/diozero-imu-devices-${version}.jar \
	diozero-imu-sampleapp/target/diozero-imu-sampleapp-${version}.jar \
	diozero-imu-visualiser/target/diozero-imu-visualiser-${version}.jar \
	diozero-remote-server/target/diozero-remote-server-${version}.jar \
	diozero-provider-bbbiolib/target/diozero-provider-bbbiolib-${version}.jar \
	diozero-provider-firmata/target/diozero-provider-firmata-${version}.jar \
	diozero-provider-jdkdio10/target/diozero-provider-jdkdio10-${version}.jar \
	diozero-provider-jdkdio11/target/diozero-provider-jdkdio11-${version}.jar \
	diozero-provider-mmap/target/diozero-provider-mmap-${version}.jar \
	diozero-provider-remote/target/diozero-provider-remote-${version}.jar \
	diozero-provider-pi4j/target/diozero-provider-pi4j-${version}.jar \
	diozero-provider-pigpio/target/diozero-provider-pigpio-${version}.jar \
	diozero-provider-voodoospark/target/diozero-provider-voodoospark-${version}.jar \
	diozero-provider-wiringpi/target/diozero-provider-wiringpi-${version}.jar \
	diozero-ws281x-java/target/diozero-ws281x-java-${version}.jar \
	diozero-sampleapps/target/diozero-sampleapps-${version}.jar \
	diozero-webapp/target/diozero-webapp-${version}.jar"


rm -f diozero-${version}.zip
zip -j target/diozero-${version}.zip $files
