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
sudo apt -y install openjdk-17-jdk-headless
```

## ZSH

```shell
sudo apt -y install zsh
chsh -s /usr/bin/zsh
sh -c "$(curl -fsSL https://raw.github.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"
```

Make a minor tweak to the robbyrussell theme to show the hostname in the command prompt:
```
cd ~/.oh-my-zsh/themes
cp robbyrussell.zsh-theme robbyrussell_tweak.zsh-theme
```

Edit `robbyrussell_tweak.zsh-theme` and change the `PROMPT` value to include this prefix `%{$fg_bold[white]%}%M%{$reset_color%} `.
The PROMPT line should look like this:

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

## Check I<sup>2</sup>C Clock Speed

```shell
#!/bin/sh
# Print current maximum i2c rate
var="$(xxd /sys/class/i2c-adapter/i2c-1/of_node/clock-frequency | awk -F': ' '{print $2}')"
var=${var//[[:blank:].\}]/}
printf "I2C Clock Rate: %d Hz\n" 0x$var
```

## Hardware PWM

All Raspberry Pi models have at least one hardware PWM channel as per the table below.

| GPIO  | PWM Channel | Func  | Models |
| :---: | :---------: | :---: | ------ |
|  12   |      0      |   4   | All except the original model A and B |
|  13   |      1      |   4   | All except the original model A and B |
|  18   |      0      |   2   | All |
|  19   |      1      |   2   | All except the original model A and B |
|  40   |      0      |   4   | CM1-3 only |
|  41   |      1      |   4   | CM1-3 only |
|  45   |      1      |   4   | CM1-3 only |
|  52   |      0      |   5   | CM1-3 only |
|  53   |      1      |   5   | CM1-3 only |

{: .note }
> These values were taken from the file `/boot/overlays/README`.

By default, configuring the hardware PWM channel is only possible via relatively complex
memory-mapped register configuration which require root access to modify. Fortunately pigpio can
handle all of this complexity for us - full hardware PWM control is available via the
[pigpioj provider](../2_concepts/1_Providers.md#pigpio).

Hardware PWM control is also possible via the Linux sysfs interface. To enable one hardware PWM
channel add `dtoverlay=pwm` to `/boot/config.txt` (using the GPIO and Func values in the table
above) and reboot. For example, to enable hardware PWM channel 0 on GPIO 12:

```
dtoverlay=pwm,pin=12,func=4
```

After a reboot the sysfs directory `/sys/class/pwm/pwmchip0/pwm0` should be present.

Alternatively, two hardware PWM channels can be configured by adding `dtoverlay=pwm-2chan` to
`/boot/config.txt`. For example, to enable hardware PWM channel 0 on GPIO 18 and channel 1 on 19:

```
dtoverlay=pwm-2chan,pin=18,func=2,pin2=19,func2=2
```

{: .danger }
> Make sure you add only one of the above and verify that the settings are correct before rebooting.

## ARMv6 - Original Pi Zero, Pi Models A/B

diozero now requires JDK-11 as a minimum. The OpenJDK version in raspbian does not run on ARMv6. 
Go to https://www.azul.com/downloads/?version=java-11-lts&os=linux&architecture=arm-32-bit-hf&package=jdk#download-openjdk

Copy the download link.

wget wget https://cdn.azul.com/zulu-embedded/bin/zulu11.60.19-ca-jdk11.0.17-linux_aarch32hf.tar.gz

cd /usr/lib/jvm

sudo tar xf zulu11.60.19-ca-jdk11.0.17-linux_aarch32hf.tar.gz
sudo ln -s zulu11.60.19-ca-jdk11.0.17-linux_aarch32hf java-11-zulu-armhf

sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/java-11-zulu-armhf/bin/javac 1

sudo update-alternatives --config java

