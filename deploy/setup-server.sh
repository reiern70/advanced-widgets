#!/bin/sh
# Prepares a fresh Lightsail Ubuntu instance: Docker, a swap file, the folder of the site, a
# firewall, SSH with keys only, security updates that restart the instance when they need to, and
# age for the backups. Run it on the instance: ssh ubuntu@<static-ip> 'sh -s' < deploy/setup-server.sh
# Every step checks what is there first, so it runs again on an instance set up before.
set -eu

# Docker and its compose plugin, from Docker's own repository
if ! command -v docker >/dev/null; then
	curl -fsSL https://get.docker.com | sudo sh
	sudo usermod -aG docker "$USER"
fi

# 1 GB swap, so a short peak of memory does not kill the JVM on a small instance
if [ ! -f /swapfile ]; then
	sudo fallocate -l 1G /swapfile
	sudo chmod 600 /swapfile
	sudo mkswap /swapfile
	sudo swapon /swapfile
	echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi

# security updates install by themselves; age encrypts the backups (see backup.sh); ufw is the
# firewall
sudo apt-get update
sudo apt-get install -y unattended-upgrades rsync age ufw

# The security updates are looked for and installed every day, and when one needs a restart of
# the instance (a new kernel), it restarts at 04:00 UTC, the server's clock, after the backup of
# 03:30 and when the site is quietest; the containers start again by themselves.
sudo tee /etc/apt/apt.conf.d/20auto-upgrades > /dev/null <<'EOF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
EOF
sudo tee /etc/apt/apt.conf.d/52antilia-widgets-reboot > /dev/null <<'EOF'
Unattended-Upgrade::Automatic-Reboot "true";
Unattended-Upgrade::Automatic-Reboot-WithUsers "true";
Unattended-Upgrade::Automatic-Reboot-Time "04:00";
EOF

# SSH with keys only, never as root: Lightsail signs in as ubuntu with a key. A file of
# sshd_config.d read before the others, as the first value of a setting counts, checked by sshd
# before it is reloaded; and only while this user has a key, not to lock anybody out.
if [ -s "$HOME/.ssh/authorized_keys" ]; then
	sudo tee /etc/ssh/sshd_config.d/10-antilia-widgets.conf > /dev/null <<'EOF'
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin no
EOF
	if sudo sshd -t; then
		sudo systemctl try-reload-or-restart ssh
	else
		sudo rm /etc/ssh/sshd_config.d/10-antilia-widgets.conf
		echo "sshd refused the hardened settings: SSH is left as it was." >&2
	fi
else
	echo "No key in $HOME/.ssh/authorized_keys: SSH is left as it was, passwords allowed." >&2
fi

# The firewall lets in SSH, HTTP, and HTTPS over TCP and over UDP (HTTP/3), nothing else. SSH is
# allowed before the firewall is turned on, so the session running this goes on. Docker opens the
# ports it publishes, 80 and 443, by itself, past ufw; the other containers publish none.
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 443/udp
sudo ufw --force enable

sudo mkdir -p /opt/antilia-widgets
sudo chown "$USER:$USER" /opt/antilia-widgets

echo "Done. Log out and in again, so the user can run docker."
