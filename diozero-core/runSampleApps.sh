#!/bin/sh

diozero_version=0.3-SNAPSHOT
#provider_classpath=diozero-provider-jdkdio11-$diozero_version.jar:dio-1.1-dev-linux-armv6hf.jar
provider_classpath=diozero-provider-pigpio-$diozero_version.jar:pigpioj-java-1.0.0.jar
log_classpath=tinylog-1.0.3.jar
library_path=-Djava.library.path=.

input_pin=12
led_pin=18
spi_cs=0
ldr_pin=3
tmp36_pin=1
mcp23017_inta_pin=21
mcp23017_intb_pin=20
mcp23017_input_pin=0
mcp23017_output_pin=1
mcp_adc_type=MCP3208

# GPIO
echo "--- Button ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.ButtonTest $input_pin
echo "--- LED ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.LEDTest $led_pin
echo "--- PWM ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.PwmTest $led_pin
echo "--- PWM LED ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.PwmLedTest $led_pin
echo "--- Button Controlled LED ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.ButtonControlledLed $input_pin $led_pin
echo "--- Random LED Flicker ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.RandomLedFlicker $led_pin

# Temperature
echo "--- BMP180 ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.BMP180Test
echo "--- MCP ADC TMP36 ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.TMP36Test $mcp_adc_type $spi_cs $tmp36_pin

# Luminosity
echo "--- TSL2561 ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.TSL2561Test
echo "--- MCP3xxx ADC using LDR pin ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.McpAdcTest $mcp_adc_type $spi_cs $ldr_pin
echo "--- MCP3xxx LDR ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.LDRTest $mcp_adc_type $spi_cs $ldr_pin
echo "--- MCP3xxx LDR with events ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.LDRListenerTest $mcp_adc_type $spi_cs $ldr_pin
echo "--- LDR Controlled LED ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.LdrControlledLed $mcp_adc_type $spi_cs $ldr_pin $led_pin

# MCP23017
echo "--- MCP23017 GPIO Expansion Input and Output ---"
sudo java -cp diozero-core-$diozero_version.jar:$log_classpath:$provider_classpath $library_path com.diozero.sampleapps.MCP23017Test $mcp23017_inta_pin $mcp23017_intb_pin $mcp23017_input_pin $mcp23017_output_pin
