---
parent: Internals
nav_order: 7
permalink: /internals/qemuaarch64.html
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
    -global virtio-blk-device.scsi=off \
    -device virtio-scsi-device,id=scsi \
    -drive file=disk.qcow2,id=rootimg,cache=unsafe,if=none \
    -device scsi-hd,drive=rootimg \
    -netdev user,id=unet -device virtio-net-device,netdev=unet \
    -net user \
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
  -device virtio-scsi-device,id=scsi \
  -drive file=disk.qcow2,id=rootimg,cache=unsafe,if=none \
  -device scsi-hd,drive=rootimg \
  -device e1000,netdev=net0 \
  -netdev user,hostfwd=tcp:127.0.0.1:2222-:22,id=net0 \
  -nographic
```

With networking (plus 8 cores and 4GB RAM):

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
sudo apt update && sudo apt -y upgrade
sudo apt -y install sudo
sudo echo "%sudo ALL=(ALL:ALL) NOPASSWD: ALL" > /etc/sudoers.d/01_sudo-nopassword
sudo usermod -aG sudo matt
```

Install some basic utilities and disable the graphical desktop:

```shell
sudo apt -y install curl gcc make unzip zip vim git zlibc build-essential libz-dev zlib1g-dev
sudo systemctl set-default multi-user.target  
```

## Java

```shell
sudo apt -y install openjdk-11-jdk
```

## ZSH

```shell
sudo apt install zsh
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
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-armhf

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

## Links

* https://blahcat.github.io/2018/01/07/building-a-debian-stretch-qemu-image-for-aarch64/
* https://florianmuller.com/raspberry-development-environment-on-macosx-with-qemu
* https://gist.github.com/humbertodias/6237f80df9a4bccf98be298057a82cf2
