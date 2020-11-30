# BeagleBone Black Setup

[Useful site](http://elinux.org/BeagleBoardDebian) for running Debian on BeagleBone boards. [Google Group](https://groups.google.com/forum/#!categories/beagleboard/beaglebone-black).

## Flash the onboard eMMC

Download the latest [Debian Jessie IoT image](https://beagleboard.org/latest-images).
Burn to a microSD card using a tool like Win32 Disk Imager or [Etcher](http://etcher.io) and boot the BeagleBone Black from the microSD card.
To turn these images into eMMC flasher images, edit the ```/boot/uEnv.txt``` file on the Linux partition on the microSD card and remove the '#' on the line with ```cmdline=init=/opt/scripts/tools/eMMC/init-eMMC-flasher-v3.sh```.
Enabling this will cause booting the microSD card to flash the eMMC.
Boot the BeagleBone with this config change and it should flash the eMMC; the LEDs will cycle for a few minutes (cylon sweep pattern).
Login and power off the device, take the microSD card out and power-on the BeagleBone - it should now boot from eMMC. Format the microSD card.

## Setup the debian user account

Edit ```/etc/sudoers.d/admin``` and change to:
```
%admin ALL=(ALL:ALL) NOPASSWD: ALL
```

Change password from 'temppwd': ```sudo passwd debian```

Add user to group gpio: ```sudo usermod -a -G gpio debian```

Edit ```~/.bashrc``` and enable colour prompt: Remove the '#' from ```force_color_prompt=yes```

## Remove the additional login messages

Edit ```/etc/issue``` (note the blank line):
```
Raspbian GNU/Linux 8 \n \l

```

Edit ```/etc/issue.net``` (no blank line):
```
Raspbian GNU/Linux 8
```

## Setup static IP address

In ```/etc/network/interfaces```:
```
connmanctl config ethernet_a0f6fd4c0e73_cable --ipv4 manual 192.168.1.16 255.255.255.0 192.168.1.254 --nameservers 192.168.1.254
```

### On other Pi-like Debian Jessie distributions

Edit ```/etc/network/interfaces```:
```
# The primary network interface
auto eth0
iface eth0 inet manual
```

```sudo apt install dhcpcd5```

Edit ```/etc/dhcpcd.conf```:
```
interface eth0
  static ip_address=192.168.1.16/24
  static routers=192.168.1.254
  static domain_name_servers=192.168.1.254
```

Enable the service: ```sudo systemctl enable dhcpcd```

## Create the update / upgrade script

Create ```/usr/local/bin/update```:
```
#!/bin/bash

apt update && apt -y upgrade && apt -y dist-upgrade
apt-get -y autoremove && apt-get -y autoclean
```

Make it executable:
```
chmod +x /usr/local/bin/upgrade
```

Run it:
```
sudo /usr/local/bin/upgrade
```

## Development library for I2C (& install unzip while we're at it)

Run: ```sudo apt install libi2c-dev unzip```

## Disable bonescript

```
sudo systemctl disable bonescript.service
sudo systemctl disable bonescript.socket
sudo systemctl disable bonescript-autorun.service
sudo systemctl disable cloud9.service
```

## Install Oracle Java

Note doesn't work as this repository is no longer active. 

```
sudo apt purge openjdk*
sudo apt-get install software-properties-common
sudo add-apt-repository "deb http://ppa.launchpad.net/webupd8team/java/ubuntu xenial main"
sudo apt-get update && sudo apt-get install oracle-java8-installer oracle-java8-set-default
```

## Locale / Timezone

Run: ```sudo dpkg-reconfigure locales```

Select ```en_GB.UTF-8```

Run: ```sudo dpkg-reconfigure tzdata```

Select: ```Europe / London```

Install and enable ntp:
```
sudo apt update && sudo apt install ntp ntpdate
sudo systemctl enable ntp
```

Check ntp: ```ntpq -p```

If need be, manually set the date / time: ```sudo date -s "07:41 04/07/2017 BST" "+%H:%M %d/%m/%Y %Z"```

## Disable HDMI / Enable SPI

Edit /boot/uEnv.txt, under ```##Example v4.1.x```:
```
cape_disable=bone_capemgr.disable_partno=BB-BONELT-HDMI,BB-BONELT-HDMIN
cape_enable=bone_capemgr.enable_partno=BB-SPIDEV0,BB-SPIDEV1
```

## Latest kernel script

```
cd /opt/scripts/tools/
git pull
sudo ./update_kernel.sh <OPTIONS>
```

## I2C Clock Frequency

Check your current I2C bus frequencies:
```
> dmesg | grep i2c
[    0.315717] omap_i2c 4802a000.i2c: bus 1 rev0.11 at 100 kHz
[    0.317272] omap_i2c 4819c000.i2c: bus 2 rev0.11 at 100 kHz
[    1.337313] i2c /dev entries driver
[    1.642672] input: tps65217_pwr_but as /devices/platform/ocp/44e0b000.i2c/i2c-0/0-0024/tps65217-pwrbutton/input/input0
[    1.644722] omap_i2c 44e0b000.i2c: bus 0 rev0.11 at 400 kHz
```

Check which dtb file you are using:
```
sudo /opt/scripts/tools/version.sh
```

Look for a line like this:
```
UBOOT: Booted Device-Tree:[am335x-boneblack-uboot-univ.dts]
```

Take a copy of your dtb file and compile to dts text format (make sure to check your kernel version):
```
cp /boot/dtbs/4.19.94-ti-r42/am335x-boneblack-uboot-univ.dtb ~/.
dtc -I dtb -O dts -o am335x-boneblack-uboot-univ.dts am335x-boneblack-uboot-univ.dtb
```

Edit the .dts file, look for the aliases section and make a note of the I2C entries. Mine had this:
```
		i2c0 = "/ocp/i2c@44e0b000";
		i2c1 = "/ocp/i2c@4802a000";
		i2c2 = "/ocp/i2c@4819c000";
```

The i2c-0 bus is not accessible on the header pins while the i2c-1 bus is utilized for reading
EEPROMS on cape add-on boards and may interfere with that function when used for other digital
I/O operations.

Then locate the i2c@xxx sections in the file to view the status ("disabled" or "okay") and clock
frequency (a hex number). Valid I2C clock frequencies (depending on the device):

* 100 kHz (100,000) : 0x186A0
* 400 kHz (400,000) : 0x61A80
* 1,000 kHz (1,000,000) : 0xF4240
* 3,400 kHz (3,400,000) : 0x33E140
* 5,000 kHz (5,000,000) : 0x4C4B40

Note the O/S only enumerates the I2C buses that are enabled (status = "okay").

My DTS file:

i2c0 (i2c@44e0b000)
```
status = "okay";
clock-frequency = < 0x61a80 >;
```

i2c1 (i2c@4802a000)
```
status = "okay";
clock-frequency = < 0x186a0 >;
```

i2c2 (/ocp/i2c@4819c000)
```
status = "okay";
clock-frequency = < 0x186a0 >;
```

Since i2c2 (/dev/i2c-2) is the bus for general usage, update the clock-frequency value to 400kHz (```clock-frequency = < 0x61a80 >;```)

Make a backup of the original dtb file. Compile the dts file back to .dtb format and copy back to /boot:
```
dtc -I dts -O dtb -o am335x-boneblack-uboot-univ.dtb am335x-boneblack-uboot-univ.dts
sudo cp am335x-boneblack-uboot-univ.dtb /boot/dtbs/4.19.94-ti-r42/.
```

Reboot and check the I2C bus speeds:
```
> dmesg | grep i2c
[    0.315730] omap_i2c 4802a000.i2c: bus 1 rev0.11 at 100 kHz
[    0.317282] omap_i2c 4819c000.i2c: bus 2 rev0.11 at 400 kHz
[    1.337240] i2c /dev entries driver
[    1.638674] input: tps65217_pwr_but as /devices/platform/ocp/44e0b000.i2c/i2c-0/0-0024/tps65217-pwrbutton/input/input0
[    1.640742] omap_i2c 44e0b000.i2c: bus 0 rev0.11 at 400 kHz
```
