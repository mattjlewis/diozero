---
parent: Single Board Computers
nav_order: 7
permalink: /boards/rock4cplus.html
---
# Rock 4C+

sudo echo "%sudo ALL=(ALL:ALL) NOPASSWD: ALL" > /etc/sudoers.d/01_sudo-nopassword

export DISTRO=bullseye-stable
curl http://apt.radxa.com/$DISTRO/public.key | sudo apt-key add -
