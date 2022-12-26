---
parent: Devices
nav_order: 6
permalink: /devices/lcds.html
redirect_from:
  - /en/latest/LCDDisplays/index.html
  - /en/stable/LCDDisplays/index.html
---

# LCDs

The basic interface is abstracted to the [LcdInterface](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/LcdInterface.html).

Connectivity is abstracted to the [LcdConnection](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/LcdConnection.html)
interface; diozero provides support for connectivity via the following GPIO expansion boards:

+ [PCF8574](3_ExpansionBoards.md#pcf8574) (a very common configuration for I<sup>2</sup>C)
+ [MCP23S17](3_ExpansionBoards.md#mcp23xxx) (as per the [PiFace Control and Display](http://www.piface.org.uk/products/piface_control_and_display/))

## HD44780 LCDs

Support for Hitachi HD44780 controlled LCDs.

HD44780Lcd [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/HD44780Lcd.html).

## GH1602

A very generic 2-line, 16-character display, included in many "experimenter" electronics and programming kits.

There appear to be several markings for this type of device:

* GH1602
* GH1602-2502
* GJD1602A-IIC

Examples of these boards:
- https://www.alibaba.com/product-detail/16-2-lines-LCD-display-Module_1600367555249.html
- https://www.alibaba.com/product-detail/3-3-V-blue-character-2x16_60469270721.html
- https://www.alibaba.com/product-detail/LCD1602-Blue-Green-screen-IIC-I2C_1600482300945.html

This implementation is directly inspired by the [Pi4J sample components](https://github.com/Pi4J/pi4j-example-components), released
under the [Apache v2 License](https://github.com/Pi4J/pi4j-example-components/blob/main/LICENSE).


GH1602 [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/com/diozero/devices/GH1602Lcd.html).
