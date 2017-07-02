# Release 0.2

+ First tagged release.

# Release 0.3

+ API change - analogue to analog.

# Release 0.4

+ Bug fixes, experimental servo support.

# Release 0.5

+ Testing improvements.

# Release 0.6

+ Stability improvements.

# Release 0.7

+ Support for non-register based I2C device read / write.

# Release 0.8

+ Added Analog Output device support (added for the PCF8591).
+ Introduced Java based sysfs and jpi providers. Bug fix to I2CLcd.
+ Added support for BME280.

# Release 0.9

+ Native support for I2C and SPI in the sysfs provider.
+ Support for CHIP, BeagleBone Black and Asus Tinker Board.
+ Moved sysfs provider into diozero-core, use as the default provider.
+ Preliminary support for devices that support the Firmata protocol (i.e. Arduino).

# Release 0.10

+ Firmata I2C.
+ Improvements to MFRC522.
+ SDL Joystick JNI wrapper library.
+ MFRC522 fully supported (finally).
+ Added support for MCP EEPROMs.
+ I2C SMBus implementation in the internal sysfs provider.

# Release 0.11 (work in progress)

+ New devices: BH1750 luminosity sensor, SSD1331 96x64 65k colour OLED
+ Moved classes in com.diozero to com.diozero.devices
+ Changed the SPI interface from ByteBuffer to byte[]
