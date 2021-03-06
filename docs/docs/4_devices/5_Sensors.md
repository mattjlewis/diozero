---
parent: Devices
nav_order: 5
permalink: /devices/sensors.html
redirect_from:
  - /en/latest/SensorComponents/index.html
  - /en/stable/SensorComponents/index.html
---

# Sensors

Sensor interfaces:
![Sensor Interfaces](/assets/images/SensorInterfaces.png)

diozero includes support for the following sensor devices:

* [Rohm BH1750](https://learn.adafruit.com/adafruit-bh1750-ambient-light-sensor) ambient light sensor
* [Bosch Sensortec BMP180](http://www.bosch-sensortec.com/bst/products/all_products/bmp180) temperature and barometric pressure sensor.
* [Bosch Sensortec BMP280](https://www.bosch-sensortec.com/products/environmental-sensors/pressure-sensors/bmp280/) temperature and barometric pressure sensor.
* [Bosch Sensortec BME280](https://www.bosch-sensortec.com/products/environmental-sensors/humidity-sensors-bme280/) relative humidity, barometric pressure and ambient temperature sensor.
* [Bosch Sensortec BME680](https://www.bosch-sensortec.com/products/environmental-sensors/gas-sensors/bme680/) gas sensor measuring relative humidity, barometric pressure, ambient temperature and gas (VOC).
* [Bosch Sensortec BME688](https://www.bosch-sensortec.com/products/environmental-sensors/gas-sensors/bme688/) 4-in-1 pressure, humidity, temperature and gas sensor.
* [Sharp GP2Y0A21YK](http://www.sharpsma.com/webfm_send/1208) distance sensor - [Datasheet](http://oomlout.com/parts/IC-PROX-01-guide.pdf).
* [HC-SR04](http://www.micropik.com/PDF/HCSR04.pdf) Ultrasonic Ranging Module.
* [STMicroelectronics HTS221](http://www2.st.com/content/ccc/resource/technical/document/datasheet/4d/9a/9c/ad/25/07/42/34/DM00116291.pdf/files/DM00116291.pdf/jcr:content/translations/en.DM00116291.pdf) Humidity and Temperature Sensor.
* [Texas Instruments LM73](https://www.ti.com/product/LM73) temperature sensor.
* [STMicroelectronics LPS25H](http://www.st.com/content/st_com/en/products/mems-and-sensors/pressure-sensors/lps25h.html) Pressure and Temperature Sensor - [Datasheet](http://www2.st.com/content/ccc/resource/technical/document/datasheet/58/d2/33/a4/42/89/42/0b/DM00066332.pdf/files/DM00066332.pdf/jcr:content/translations/en.DM00066332.pdf).
* [Mifare MFRC522](https://www.nxp.com/documents/data_sheet/MFRC522.pdf) RFID card.
* [Sensirion SGP30](https://www.sensirion.com/en/environmental-sensors/gas-sensors/sgp30/) VOC / indoor air quality sensor.
* TMP36 temperature sensor.
* [ams TSL2561](https://ams.com/tsl2561) Digital Luminosity / Lux / Light Sensor - [Datasheet](https://www.adafruit.com/datasheets/TSL2561.pdf).
* 1-wire temperature sensors such as the DS18B20. Adafruit has a good [article on connecting these](https://learn.adafruit.com/adafruits-raspberry-pi-lesson-11-ds18b20-temperature-sensing?view=all) to the Raspberry Pi. Currently supported types: DS18S20, DS1822, DS18B20, DS1825, DS28EA00, MAX31850K.
