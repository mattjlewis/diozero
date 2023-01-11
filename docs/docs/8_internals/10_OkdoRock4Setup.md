sudo echo "%sudo ALL=(ALL:ALL) NOPASSWD: ALL" > /etc/sudoers.d/01_sudo-nopassword

export DISTRO=bullseye-stable
curl http://apt.radxa.com/$DISTRO/public.key | sudo apt-key add -

sudo apt update
sudo apt full-upgrade

sudo nmcli r wifi on
sudo nmcli dev wifi connect <<ssid>> password <<password>>
sudo systemctl set-default multi-user.target

sudo apt install locales

sudo localectl set-locale en_GB.UTF-8

Or run: `sudo dpkg-reconfigure locales`

Select: `en_GB.UTF-8`

sudo timedatectl set-timezone Europe/London

Or - run: `sudo dpkg-reconfigure tzdata`

Select: `Europe / London`


Add gpio group:

```
sudo groupadd gpio
sudo usermod -aG gpio rock
```

## Development Libraries and Tools

```
sudo apt -y install i2c-tools libi2c-dev gpiod libgpiod2 libgpiod-dev avahi-daemon gcc make unzip zip vim git
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
