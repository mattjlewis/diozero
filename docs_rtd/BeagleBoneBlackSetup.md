# Beaglebone Black Setup

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
