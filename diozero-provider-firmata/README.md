# Firmata Provider

Implementation of the [Firmata protocol](https://github.com/firmata/protocol) as a diozero provider.
Can operate via either serial or sockets communication. The FirmataAdapter classes are all within
the `com.diozero.internal.provider.firmata.adapter` package and can be used independently of the diozero
API.

Supported features:

* Device discovery
    * [Query firmware name and version](https://github.com/firmata/protocol/blob/master/protocol.md#query-firmware-name-and-version)
    * [Capability query](https://github.com/firmata/protocol/blob/master/protocol.md#capability-query)
    * [Pin state query](https://github.com/firmata/protocol/blob/master/protocol.md#pin-state-query_
    * [Analog mapping query](https://github.com/firmata/protocol/blob/master/protocol.md#analog-mapping-query)
* Digital input / output
* Analog input
* Digital & analog events with [configurable sampling interval](https://github.com/firmata/protocol/blob/master/protocol.md#sampling-interval)
* PWM output
* [Servo](https://github.com/firmata/protocol/blob/master/servos.md)
* [I<sup>2</sup>C](https://github.com/firmata/protocol/blob/master/i2c.md)
* [Scheduler](https://github.com/firmata/protocol/blob/master/scheduler.md)
