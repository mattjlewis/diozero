---
title: Exceptions
parent: API
nav_order: 5
permalink: /api/exceptions.html
---

# Exceptions

diozero makes use of unchecked exceptions and uses RuntimeIOException as the base class for
the majority of exceptions that can be thrown.
The use of unchecked exceptions over checked exceptions isn't always clear, the
[Oracle Java Documentation](https://docs.oracle.com/javase/tutorial/essential/exceptions/runtime.html)
provides the following guidance:

_"If a client can reasonably be expected to recover from an exception, make it a checked exception. If a client cannot do anything to recover from the exception, make it an unchecked exception.”_ 

Broadly, RuntimeIOException is thrown as a result of the following incomplete list of scenarios:

* General - any file IOException gets rethrown as a wrapped RuntimeIOException
* General - any errors returned from the native C library (status value < 0)
* BME280 - if the I2C device at the specified address does not report the correct device id when reading the ID register
* McpEeprom - if an I2C probe doesn’t detect the device at the specified address
* W1ThermSensor - if the contents of the kernel file do not match what is expected
* SysFsAnalogInputDevice - if there is a NumberFormatException reading the kernel file (that is already a runtime unchecked exception so I could just let that one propagate)

The following exceptions are also thrown:

Java IllegalArgumentException
: If the input parameter combination doesn’t make sense, the value is out of range (e.g. greater than one for PWM), or you requested a GPIO / ADC number on an expansion board that doesn’t exist

diozero InvalidModeException
: If the GPIO device doesn’t support the request mode

Java IllegalStateException
: DigitalInputOutputDevice if you attempt to set the value when currently in input mode

Java UnsupportedOperationException
: When a feature isn’t supported by a provider

diozero DeviceAlreadyOpenedException
: An attempt to open a device that is already provisioned and open

diozero DeviceBusyException
: I2C specific error (-16 EBUSY)

diozero NoSuchDeviceException:
: If the requested device isn't found
