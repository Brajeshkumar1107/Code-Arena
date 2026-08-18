#!/usr/bin/env bash
# setup-vm.sh — Bootstrap an Oracle Cloud Always Free ARM VM for CodeArena.
#
# Run this once after SSH-ing into a fresh Ubuntu 22.04 ARM VM:
#   chmod +x setup-vm.sh && ./setup-vm.sh
set -euo pipefail

echo "=== CodeArena VM Setup ==="
echo "Architecture: $(uname -m)"
echo "OS: $(lsb_release -ds 2>/dev/null || cat /etc/os-release | grep PRETTY_NAME | cut -d= -f2)"
echo ""

# ---------- System updates ----------
echo "[1/6] Updating system packages..."
sudo apt-get update -qq
sudo apt-get upgrade -y -qq

# ---------- Swap (2 GB) ----------
echo "[2/6] Setting up swap..."
if [ ! -f /swapfile ]; then
    sudo fallocate -l 2G /swapfile
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
    echo "  2 GB swap created"
else
    echo "  Swap already exists"
fi

# ---------- Docker ----------
echo "[3/6] Installing Docker..."
if command -v docker &>/dev/null; then
    echo "  Docker already installed: $(docker --version)"
else
    sudo apt-get install -y -qq \
        apt-transport-https ca-certificates curl software-properties-common
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker.gpg] \
        https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
        sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
    sudo apt-get update -qq
    sudo apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin
    sudo systemctl enable --now docker
    echo "  Docker installed: $(docker --version)"
fi

# Add current user to docker group (avoids needing sudo for docker commands)
if ! groups "$USER" | grep -q docker; then
    sudo usermod -aG docker "$USER"
    echo "  Added $USER to docker group (log out and back in for this to take effect)"
fi

# ---------- Workspace directory ----------
echo "[4/6] Creating workspace directory..."
sudo mkdir -p /var/lib/codearena/workspaces
sudo chown 1000:1000 /var/lib/codearena/workspaces
echo "  /var/lib/codearena/workspaces ready"

# ---------- Firewall ----------
echo "[5/6] Configuring firewall..."
if command -v ufw &>/dev/null; then
    sudo ufw allow 22/tcp   >/dev/null 2>&1 || true
    sudo ufw allow 8081/tcp >/dev/null 2>&1 || true
    sudo ufw --force enable >/dev/null 2>&1 || true
    echo "  UFW: ports 22 and 8081 open"
else
    echo "  ufw not installed — open port 8081 manually in Oracle Cloud Security List"
fi

# ---------- Auto updates ----------
echo "[6/6] Enabling unattended security updates..."
if [ ! -f /etc/apt/apt.conf.d/20auto-upgrades ]; then
    echo 'APT::Periodic::Update-Package-Lists "1";' | sudo tee /etc/apt/apt.conf.d/20auto-upgrades >/dev/null
    echo 'APT::Periodic::Unattended-Upgrade "1";'    | sudo tee /etc/apt/apt.conf.d/20auto-upgrades >/dev/null
fi

echo ""
echo "=== Setup complete ==="
echo ""
echo "Next steps:"
echo "  1. Log out and back in (for docker group to take effect)"
echo "  2. Clone or scp the CodeArena project"
echo "  3. Run deploy.sh"
echo ""
echo "Memory: $(free -h | awk '/Mem:/ {print $3 "/" $2}')"
echo "Swap:   $(free -h | awk '/Swap:/ {print $3 "/" $2}')"
echo "Disk:   $(df -h / | awk 'NR==2 {print $3 "/" $2 " (" $5 " used)"}')"
