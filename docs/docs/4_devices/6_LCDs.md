---
parent: Devices
nav_order: 6
permalink: /devices/lcds.html
redirect_from:
  - /en/latest/LCDDisplays/index.html
  - /en/stable/LCDDisplays/index.html
---

# LCDs

## HD44780 LCDs

Support for Hitachi HD44780 controlled LCDs.

HD44780Lcd [Javadoc](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/devices/HD44780Lcd.html).

Connectivity is abstracted to the [HD44780.LcdConnection](https://www.javadoc.io/doc/com.diozero/diozero-core/latest/diozero.core/com/diozero/devices/LcdConnection.html)
interface; diozero provides support for connectivity via the following GPIO expansion boards:

+ [PCF8574](3_ExpansionBoards.md#pcf8574) (a very common configuration)
+ [MCP23S17](3_ExpansionBoards.md#mcp23xxx) (as per the [PiFace Control and Display](http://www.piface.org.uk/products/piface_control_and_display/))
