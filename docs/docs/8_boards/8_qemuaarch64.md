---
parent: Single Board Computers
nav_order: 8
permalink: /boards/qemuaarch64.html
redirect_from:
  - /internals/qemuaarch64.html
---

# Building a Debian Buster QEMU image for AARCH64

Based on this article on [building a Debian Stretch QEMU image for AARCH64](https://blahcat.github.io/2018/01/07/building-a-debian-stretch-qemu-image-for-aarch64/).

Get base files (Debian Buster network installer image):

```shell
wget http://ftp.debian.org/debian/dists/buster/main/installer-arm64/current/images/netboot/debian-installer/arm64/initrd.gz
wget http://ftp.debian.org/debian/dists/buster/main/installer-arm64/current/images/netboot/debian-installer/arm64/linux
```

Create a disk:

```shell
qemu-img create -f qcow2 disk.qcow2 20G
```

Install Debian:

```shell
qemu-system-aarch64 -smp 2 -M virt -cpu cortex-a57 -m 1G \
    -initrd initrd.gz -kernel linux \
    -append "root=/dev/ram console=ttyAMA0" \
    -device virtio-scsi-device \
    -blockdev qcow2,node-name=hd0,file.driver=file,file.filename=disk.qcow2 \
    -device scsi-hd,drive=hd0 \
    -netdev user,id=unet -device virtio-net-device,netdev=unet \
    -nographic
```

You need to extract the initrd.img and vmlinuz files from the qcow2 disk file.
The easiest way to do this is to use `nbd` in a Linux environment,
e.g. a Virtual Machine that has access to the qcow2 disk file:

```shell
sudo apt install nbd-client qemu qemu-utils
sudo modprobe nbd max_part=8
sudo qemu-nbd --connect=/dev/nbd0 disk.qcow2
sudo mount /dev/nbd0p1 /media/qcow
sudo cp initrd.img-4.19.0-16-arm64 vmlinuz-4.19.0-16-arm64 ../qemu/.
sync
sudo umount /dev/nbd0p1
sudo nbd-client -d /dev/nbd0
```

Then:

```shell
qemu-system-aarch64 -smp 2 -M virt -cpu cortex-a57 -m 1G \
  -initrd initrd.img-4.19.0-16-arm64 \
  -kernel vmlinuz-4.19.0-16-arm64 \
  -append "root=/dev/sda2 console=ttyAMA0" \
  -device virtio-scsi-device \
  -blockdev qcow2,node-name=hd0,file.driver=file,file.filename=disk.qcow2 \
  -device scsi-hd,drive=hd0 \
  -netdev user,id=unet -device virtio-net-device,netdev=unet \
  -nographic
```

With additional resources allocated (8 cores and 4GB RAM):

```shell
qemu-system-aarch64 -smp 8 -M virt -cpu cortex-a57 -m 4G \
  -initrd initrd.img-4.19.0-16-arm64 \
  -kernel vmlinuz-4.19.0-16-arm64 \
  -append "root=/dev/sda2 console=ttyAMA0" \
  -device virtio-scsi-device \
  -blockdev qcow2,node-name=hd0,file.driver=file,file.filename=disk.qcow2 \
  -device scsi-hd,drive=hd0 \
  -netdev user,id=unet -device virtio-net-device,netdev=unet \
  -nographic
```

Login and setup sudo:

```shell
su - -c "apt -y install sudo"
su - -c "usermod -aG sudo matt"
exit
```

Login again:

```shell
sudo apt update && sudo apt -y upgrade
sudo echo "%sudo ALL=(ALL:ALL) NOPASSWD: ALL" > /etc/sudoers.d/01_sudo-nopassword
```

Install some basic utilities and disable the graphical desktop:

```shell
sudo apt -y install curl gcc make unzip zip vim git build-essential libz-dev zlib1g-dev
sudo systemctl set-default multi-user.target
```

## Kernel Version

Install a [newer kernel version](https://jensd.be/968/linux/install-a-newer-kernel-in-debian-10-buster-stable) in Debian Buster.

## Links and References

* [Building a Debian Stretch QEMU Image for AArch64](https://blahcat.github.io/2018/01/07/building-a-debian-stretch-qemu-image-for-aarch64/)
* [Raspberry Development Environment on MacOSX with QEMU](https://florianmuller.com/raspberry-development-environment-on-macosx-with-qemu)
* [QEMU with ARM on Mac OSX](https://gist.github.com/humbertodias/6237f80df9a4bccf98be298057a82cf2)
