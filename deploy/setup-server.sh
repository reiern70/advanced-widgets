#!/bin/sh
# Prepares a fresh Lightsail Ubuntu instance, once: Docker, a swap file and the folder of the
# site. Run it on the instance: ssh ubuntu@<static-ip> 'sh -s' < deploy/setup-server.sh
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

# security updates install by themselves
sudo apt-get update
sudo apt-get install -y unattended-upgrades rsync

sudo mkdir -p /opt/advanced-widgets
sudo chown "$USER:$USER" /opt/advanced-widgets

echo "Done. Log out and in again, so the user can run docker."
