###
# #%L
# Organisation: mattjlewis
# Project:      Device I/O Zero
# Filename:     runSampleApps.sh  
# 
# This file is part of the diozero project. More information about this project
# can be found at http://www.diozero.com/
# %%
# Copyright (C) 2016 - 2019 mattjlewis
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

#provider_classpath=diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar
provider_classpath=diozero-provider-pigpio-${DIOZERO_VERSION}.jar:pigpioj-java-2.0.jar
log_classpath=tinylog-1.2.jar
library_path=-Djava.library.path=.

input_pin=25
led_pin=12

# MCP ADC Config
spi_cs=0
mcp_adc_type=MCP3304
pot_pin=1
ldr_pin=2
dist_pin=3
tmp36_2_pin=5
tmp36_pin=7

mcp23017_inta_pin=21
mcp23017_intb_pin=20
mcp23017_input_pin=0
mcp23017_output_pin=1

# GPIO
echo "--- Button ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.ButtonTest $input_pin
echo "--- LED ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.LEDTest $led_pin
echo "--- PWM ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.PwmTest $led_pin
echo "--- PWM LED ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.PwmLedTest $led_pin
echo "--- Button Controlled LED ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.ButtonControlledLed $input_pin $led_pin
echo "--- Random LED Flicker ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.RandomLedFlicker $led_pin

# Temperature
echo "--- BMP180 ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.BMP180Test
echo "--- MCP ADC TMP36 ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.TMP36Test $mcp_adc_type $spi_cs $tmp36_pin

# Luminosity
echo "--- TSL2561 ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.TSL2561Test
echo "--- MCP3xxx ADC using LDR pin ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.McpAdcTest $mcp_adc_type $spi_cs $ldr_pin
echo "--- MCP3xxx LDR ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.LDRTest $mcp_adc_type $spi_cs $ldr_pin
echo "--- MCP3xxx LDR with events ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.LDRListenerTest $mcp_adc_type $spi_cs $ldr_pin
echo "--- LDR Controlled LED ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.LdrControlledLed $mcp_adc_type $spi_cs $ldr_pin $led_pin

# MCP23017
echo "--- MCP23017 GPIO Expansion Input and Output ---"
sudo java -cp diozero-core-$DIOZERO_VERSION.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.MCP23017Test $mcp23017_inta_pin $mcp23017_intb_pin $mcp23017_input_pin $mcp23017_output_pin
