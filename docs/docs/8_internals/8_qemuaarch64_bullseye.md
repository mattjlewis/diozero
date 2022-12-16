---
parent: Internals
nav_order: 8
permalink: /internals/qemuaarch64_bullseye.html
---

# Building a Debian Bullseye QEMU image for AARCH64

Based on this article on [building a Debian Stretch QEMU image for AARCH64](https://blahcat.github.io/2018/01/07/building-a-debian-stretch-qemu-image-for-aarch64/).

Get base files (Debian Bullseye network installer image):

```shell
wget -O installer-initrd.gz http://ftp.debian.org/debian/dists/bullseye/main/installer-arm64/current/images/netboot/debian-installer/arm64/initrd.gz
wget -O installer-linux http://ftp.debian.org/debian/dists/bullseye/main/installer-arm64/current/images/netboot/debian-installer/arm64/linux
```

Create a disk:

```shell
qemu-img create -f qcow2 disk.qcow2 20G
```

Install Debian:

```shell
qemu-system-aarch64 -smp 2 -M virt -cpu cortex-a57 -m 1G \
    -initrd installer-initrd.gz -kernel installer-linux \
    -append "root=/dev/ram" \
    -device virtio-scsi-device \
    -blockdev qcow2,node-name=hd0,file.driver=file,file.filename=disk.qcow2 \
    -device scsi-hd,drive=hd0 \
    -netdev user,id=unet -device virtio-net-device,netdev=unet \
    -nographic -no-reboot
```

TBD: Replace with libguestfs.
You need to extract the initrd.img and vmlinuz files from the qcow2 disk file.
The easiest way to do this is to use `nbd` in a Linux environment,
e.g. a Virtual Machine that has access to the qcow2 disk file:

```shell
sudo apt install nbd-client qemu qemu-utils
sudo modprobe nbd max_part=8
sudo qemu-nbd --connect=/dev/nbd0 disk.qcow2
sudo mount /dev/nbd0p1 /media/qcow
sudo cp initrd.img-5.10.0-17-arm64 vmlinuz-5.10.0-17-arm64 ../qemu/.
sync
sudo umount /dev/nbd0p1
sudo nbd-client -d /dev/nbd0
```

Then:

```shell
qemu-system-aarch64 -smp 2 -M virt -cpu cortex-a57 -m 1G \
  -initrd initrd.img-5.10.0-17-arm64 \
  -kernel vmlinuz-5.10.0-17-arm64 \
  -append "root=/dev/sda2 console=ttyAMA0" \
  -device virtio-scsi-device \
  -blockdev qcow2,node-name=hd0,file.driver=file,file.filename=disk.qcow2 \
  -device scsi-hd,drive=hd0 \
  -netdev user,id=unet -device virtio-net-device,netdev=unet \
  -nographic
```

With additional resources allocated (8 cores, 4GB RAM and guest port 22 forwarded to host 5555):

```
qemu-system-aarch64 -smp 8 -M virt -cpu cortex-a57 -m 4G \
  -initrd initrd.img-5.10.0-17-arm64 -kernel vmlinuz-5.10.0-17-arm64 \
  -append "root=/dev/sda2" \
  -device virtio-scsi-device \
  -device scsi-hd,drive=hd0 \
  -blockdev qcow2,node-name=hd0,file.driver=file,file.filename=disk.qcow2 \
  -netdev user,id=net0,hostfwd=tcp::5555-:22 -device virtio-net-device,netdev=net0 \
  -nographic
```

Remote login:

```shell
ssh-copy-id -i ~/.ssh/diozero matt@localhost:5555
```

Remote copy: `scp -P 5555 some_file matt@localhost:/home/matt/some_file`

Login and setup sudo:

```shell
ssh -p 5555 matt@localhost
su - -c "apt -y install sudo"
su - -c "usermod -aG sudo matt"
exit
```

Login again:

```shell
sudo echo "%sudo ALL=(ALL:ALL) NOPASSWD: ALL" > /etc/sudoers.d/01_sudo-nopassword
sudo apt update && sudo apt -y full-upgrade
```

Install some basic utilities and disable the graphical desktop:

```shell
sudo apt -y install curl gcc make unzip zip vim git build-essential libz-dev zlib1g-dev
sudo systemctl set-default multi-user.target
```

## Java & GraalVM

```shell
sudo apt -y install openjdk-17-jdk-headless maven
wget -O - https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.2.0/graalvm-ce-java17-linux-aarch64-22.2.0.tar.gz | tar zxf -
./graalvm-ce-java17-22.2.0/bin/gu install native-image
```

## ZSH

```shell
sudo apt -y install zsh
chsh -s /usr/bin/zsh
sh -c "$(curl -fsSL https://raw.github.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"
```

Make a minor tweak to the robbyrussell theme to show the hostname in the command prompt:

```shell
cd ~/.oh-my-zsh/themes
cp robbyrussell.zsh-theme robbyrussell_tweak.zsh-theme
```

Edit `robbyrussell_tweak.zsh-theme` and change the `PROMPT` value to include this prefix `%{$fg_bold[white]%}%M%{$reset_color%} `:

```
PROMPT="%{$fg_bold[white]%}%M%{$reset_color%} %(?:%{$fg_bold[green]%}➜ :%{$fg_bold[red]%}➜ )"
```

Update the ZSH config `~/.zshrc`:

```
export PATH=$PATH:/sbin:/usr/sbin:/usr/local/sbin
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-armhf
export GRAALBM_HOME=/home/matt/graalvm-ce-java17-22.2.0

ZSH_THEME="robbyrussell_tweak"
```

My own preference is to add this to the end of the `.zshrc` file:

```
# Allow multiple terminal sessions to all append to one zsh command history
setopt APPEND_HISTORY
# Do not enter command lines into the history list if they are duplicates of the previous event
setopt HIST_IGNORE_DUPS
# Remove command lines from the history list when the first character on the line is a space
setopt HIST_IGNORE_SPACE
# Remove the history (fc -l) command from the history list when invoked
setopt HIST_NO_STORE
```

## Kernel Version

Install a [newer kernel version](https://jensd.be/968/linux/install-a-newer-kernel-in-debian-10-buster-stable) in Debian Buster.

## Links and References

* [Building a Debian Stretch QEMU Image for AArch64](https://blahcat.github.io/2018/01/07/building-a-debian-stretch-qemu-image-for-aarch64/)
* [Raspberry Development Environment on MacOSX with QEMU](https://florianmuller.com/raspberry-development-environment-on-macosx-with-qemu)
* [QEMU with ARM on Mac OSX](https://gist.github.com/humbertodias/6237f80df9a4bccf98be298057a82cf2)
* [Debian on QEMU-emulated ARM-64 aarch64](http://phwl.org/2022/qemu-aarch64-debian/)
