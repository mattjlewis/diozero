#!/usr/bin/python3

###
# #%L
# Organisation: diozero
# Project:      Device I/O Zero - Core
# Filename:     CamJamMotorTest.py  
# 
# This file is part of the diozero project. More information about this project
# can be found at http://www.diozero.com/
# %%
# Copyright (C) 2016 - 2020 diozero
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

from gpiozero import LED
from gpiozero import DigitalInputDevice
from gpiozero import CamJamKitRobot
import time
import sys

PIN_REAR_LEFT_LED = 22
PIN_REAR_RIGHT_LED = 27
PIN_FRONT_LEFT_LED = 18
PIN_FRONT_RIGHT_LED = 17

PIN_LEFT_IR_SENSOR = 24
PIN_CENTRE_IR_SENSOR = 23
PIN_RIGHT_IR_SENSOR = 25

GO_FORWARDS = 1
GO_BACKWARDS = 2
GO_LEFT = 3
GO_RIGHT = 4
GO_LEFT_BACKWARDS = 5
GO_RIGHT_BACKWARDS = 6
GO_SHARP_LEFT = 7
GO_SHARP_RIGHT = 8

front_left_led = LED(PIN_FRONT_LEFT_LED, initial_value=False)
front_right_led = LED(PIN_FRONT_RIGHT_LED, initial_value=False)
rear_left_led = LED(PIN_REAR_LEFT_LED, initial_value=False)
rear_right_led = LED(PIN_REAR_RIGHT_LED, initial_value=False)

left_ir_sensor = DigitalInputDevice(PIN_LEFT_IR_SENSOR)
centre_ir_sensor = DigitalInputDevice(PIN_CENTRE_IR_SENSOR)
right_ir_sensor = DigitalInputDevice(PIN_RIGHT_IR_SENSOR)

robot = CamJamKitRobot()

def go(direction, duration=0, speed=1):
  if direction == GO_FORWARDS:
    front_left_led.on()
    front_right_led.on()
    rear_left_led.off()
    rear_right_led.off()
    robot.forward(speed)
  elif direction == GO_BACKWARDS:
    front_left_led.off()
    front_right_led.off()
    rear_left_led.on()
    rear_right_led.on()
    robot.backward(speed)
  elif direction == GO_SHARP_LEFT:
    front_left_led.off()
    front_right_led.on()
    rear_left_led.off()
    rear_right_led.on()
    robot.left(speed)
  elif direction == GO_SHARP_RIGHT:
    front_left_led.on()
    front_right_led.off()
    rear_left_led.on()
    rear_right_led.off()
    robot.right(speed)
  elif direction == GO_LEFT:
    front_left_led.off()
    front_right_led.on()
    rear_left_led.off()
    rear_right_led.off()
    robot.left_motor.stop()
    robot.right_motor.forward(speed)
  elif direction == GO_RIGHT:
    front_left_led.on()
    front_right_led.off()
    rear_left_led.off()
    rear_right_led.off()
    robot.left_motor.forward(speed)
    robot.right_motor.stop()
  elif direction == GO_LEFT_BACKWARDS:
    front_left_led.off()
    front_right_led.off()
    rear_left_led.off()
    rear_right_led.on()
    robot.left_motor.stop()
    robot.right_motor.backward(speed)
  elif direction == GO_RIGHT_BACKWARDS:
    front_left_led.off()
    front_right_led.off()
    rear_left_led.on()
    rear_right_led.off()
    robot.left_motor.backward(speed)
    robot.right_motor.stop()
  if duration > 0:
    time.sleep(duration)

def stop(duration=0):
  front_left_led.off()
  front_right_led.off()
  rear_left_led.off()
  rear_right_led.off()
  robot.stop()
  if duration > 0:
    time.sleep(duration)

def main():
  front_left_led.blink(0.25, 0.25, 1, False)
  front_right_led.blink(0.25, 0.25, 1, False)
  rear_right_led.blink(0.25, 0.25, 1, False)
  rear_left_led.blink(0.25, 0.25, 1, False)
  time.sleep(1)

  left_ir_sensor.when_activated = lambda: print("Left activated")
  left_ir_sensor.when_deactivated = lambda: print("Left deactivated")
  centre_ir_sensor.when_activated = lambda: print("Centre activated")
  centre_ir_sensor.when_deactivated = lambda: print("Centre deactivated")
  right_ir_sensor.when_activated = lambda: print("Right activated")
  right_ir_sensor.when_deactivated = lambda: print("Right deactivated")

  DELAY = 0.65
  speed=0.5

  go(GO_RIGHT, DELAY, speed)
  go(GO_FORWARDS, DELAY, speed)
  go(GO_LEFT, DELAY, speed)
  stop(DELAY)
  go(GO_LEFT_BACKWARDS, DELAY, speed)
  go(GO_BACKWARDS, DELAY, speed)
  go(GO_RIGHT_BACKWARDS, DELAY, speed)
  stop(DELAY)
  go(GO_SHARP_LEFT, DELAY, speed)
  go(GO_SHARP_RIGHT, DELAY, speed)
  stop(DELAY)
  go(GO_FORWARDS, DELAY, speed)
  go(GO_BACKWARDS, DELAY, speed)

if __name__ == "__main__":
  main()
