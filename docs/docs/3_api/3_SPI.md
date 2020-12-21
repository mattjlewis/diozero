---
title: SPI Devices
parent: API
nav_order: 3
permalink: /api/spi.html
---

# Serial Peripheral Interface (SPI) Devices

*class* **com.diozero.api.SpiDevice**{: .descname } (*controller*, *chipSelect*, *frequency*, *mode*, *lsbFirst*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/api/SpiDevice.java){: .viewcode-link } [&para;](API.md#spi-support "Permalink to this definition"){: .headerlink }

: Class for reading / writing to SPI devices.

    * **controller** (*int*) - SPI controller.
    
    * **chipSelect** (*int*) - Chip Select, aka Chip Enable.
    
    * **frequency** (*int*) - SPI frequency in HZ.
    
    * **mode** (*SpiClockMode*) - SPI clock mode to be used, 1 for each combination of Clock Polarity / Clock Phase. See [Wikiepedia](https://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus#Mode_number) for further information.
    
    * **lsbFirst** (*boolean*) - Byte order to use in communication.
    
    **write** (*out*)
    
    : Write data to the device.

    * **out** (*ByteBuffer*) - the data to write.
    
    *ByteBuffer* **writeAndRead** (*out*)
    
    : Write and then read data to the device in the same transaction. The number of bytes read is the same as that written. Maximum output buffer size is 2048 bytes.

    * **out** (*ByteBuffer*) - the data to write.
