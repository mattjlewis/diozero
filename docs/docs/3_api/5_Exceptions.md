---
title: Exceptions
parent: API
nav_order: 4
permalink: /api/exceptions.html
---

# Exceptions

These are the two main reasons that led me to using the unchecked RuntimeIOException. I should really create some derived types for the various different scenarios that can occur. I do openly admit that it is currently extremely broad, I’ve just done a search for “throw new RuntimeIOException” and can see the following incomplete list of scenarios:
General - any file IOException gets rethrown as a wrapped RuntimeIOException
General - any errors returned from the native C library (status value < 0)
BME280 - if the I2C device at the specified address does not report the correct device id when reading the ID register
McpEeprom - if an I2C probe doesn’t detect the device at the specified address
W1ThermSensor - if the contents of the kernel file do not match what is expected
SysFsAnalogInputDevice - if there is a NumberFormatException reading the kernel file (that is already a runtime unchecked exception so I could just let that one propagate)

That’s about it actually for diozero-core.

I do also throw the following exceptions:
Java IllegalArgumentException - if the input parameter combination doesn’t make sense, the value is out of range (e.g. greater than one for PWM), or you requested a GPIO / ADC number on an expansion board that doesn’t exist
diozero InvalidModeException - if the GPIO device doesn’t support the request mode
Java IllegalStateException - DigitalInputOutputDevice if you attempt to set the value when currently in input mode
Java UnsupportedOperationException - when a feature isn’t supported by a provider
diozero DeviceAlreadyOpenedException - an attempt to open a device that is already provisioned and open
diozero DeviceBusyException - I2C specific error (-16 EBUSY)
