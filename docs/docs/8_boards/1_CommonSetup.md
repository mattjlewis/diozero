---
parent: Single Board Computers
nav_order: 1
permalink: /boards/commonsetup.html
---
# Common Setup

## User Permissions

```shell
sudo usermod -a -G dialout,gpio,i2c,spi <<username>>
```

My own personal preference is to not require a password when issuing a sudo command (for those in the `sudo` group):

```shell
sudo sh -c 'echo "%sudo ALL=(ALL:ALL) NOPASSWD: ALL" > /etc/sudoers.d/01_sudo-nopassword'
```

## Development Libraries and Tools

```shell
sudo apt -y install avahi-daemon gcc make unzip zip vim git
sudo apt -y install i2c-tools libi2c-dev gpiod libgpiod2 libgpiod-dev libpigpiod-if-dev libpigpiod-if2-1
```

## Java

```shell
sudo apt -y install openjdk-17-jdk-headless
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

Edit `robbyrussell_tweak.zsh-theme` and change the `PROMPT` value to include this prefix `%{$fg_bold[white]%}%M%{$reset_color%} `.
The PROMPT line should look like this:

```shell
PROMPT="%{$fg_bold[white]%}%M%{$reset_color%} %(?:%{$fg_bold[green]%}➜ :%{$fg_bold[red]%}➜ )"
```

Update the ZSH config `~/.zshrc`:

```shell
export PATH=$PATH:/sbin:/usr/sbin:/usr/local/sbin
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-armhf

ZSH_THEME="robbyrussell_tweak"
```

My own preference is to add this to the end of the `.zshrc` file:

```shell
# Allow multiple terminal sessions to all append to one zsh command history
setopt APPEND_HISTORY
# Do not enter command lines into the history list if they are duplicates of the previous event
setopt HIST_IGNORE_DUPS
# Remove command lines from the history list when the first character on the line is a space
setopt HIST_IGNORE_SPACE
# Remove the history (fc -l) command from the history list when invoked
setopt HIST_NO_STORE
```

## System Update / Upgrade Script

Create `/usr/local/sbin/system-update`:

```
#!/bin/sh

apt update && apt -y --auto-remove full-upgrade
apt -y autoclean
```

Make it executable: `sudo chmod +x /usr/local/sbin/system-update`

## Locale / Timezone

```
sudo apt install locales
sudo localectl set-locale en_GB.UTF-8
sudo timedatectl set-timezone Europe/London
```

Run: `sudo dpkg-reconfigure locales`

Select the appropriate locale, e.g. `en_GB.UTF-8`

Run: `sudo dpkg-reconfigure tzdata`

Select the appropriate timezone, e.g. `Europe / London`

## Enable / Disable Graphical Desktop

Pick one:

```shell
sudo systemctl set-default multi-user.target
sudo systemctl set-default graphical.target
```

## Connect to WiFi

```shell
sudo nmcli r wifi on
sudo nmcli dev wifi connect <<ssid>> password <<password>>
```
