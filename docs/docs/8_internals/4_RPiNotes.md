---
parent: Internals
nav_order: 4
permalink: /internals/rpinotes.html
---

# Raspberry Pi Setup Notes

These notes are based on an install of the 32-bit Raspberry Pi OS Lite.

## User Permissions

Note that this shouldn't be required - by default the pi user is in the dialout, gpio, i2c and spi groups.

```shell
sudo usermod -a -G dialout pi
sudo usermod -a -G i2c pi
```

## Development Libraries and Tools

```shell
sudo apt -y install i2c-tools libi2c-dev gpiod libgpiod2 libgpiod-dev avahi-daemon gcc make unzip zip vim git
sudo apt -y install pigpio-tools libpigpiod-if-dev libpigpiod-if2-1
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
```
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

## Check I2C Clock Speed

```shell
#!/bin/sh
# Print current maximum i2c rate
var="$(xxd /sys/class/i2c-adapter/i2c-1/of_node/clock-frequency | awk -F': ' '{print $2}')"
var=${var//[[:blank:].\}]/}
printf "I2C Clock Rate: %d Hz\n" 0x$var
```
