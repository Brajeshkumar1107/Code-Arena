# Deploy CodeArena to Oracle Cloud Always Free

## What You Get (Free Forever)

| Resource | Allocation |
|----------|-----------|
| Compute | 4 ARM OCPUs (Ampere A1) + 24 GB RAM |
| Storage | 200 GB boot volume |
| Network | 10 TB outbound/month |
| Public IP | 1 reserved (static) |

> **Important**: The Always Free tier requires a Pay-As-You-Go account upgrade.
> Your credit card is only used for identity verification. You will never be charged
> as long as you stay within Always Free resources.

---

## Architecture

```
Oracle Cloud ARM VM (Ubuntu 22.04)
├── Docker Engine
├── docker-compose.yml
│   ├── MySQL 8.4        (~1 GB RAM)
│   ├── Kafka KRaft       (~512 MB RAM)
│   └── CodeArena Runner  (~1 GB RAM)
│       └── /var/run/docker.sock → host Docker
│           └── Sandboxed child containers:
│               ├── eclipse-temurin:21-jdk  (Java 21)
│               ├── gcc:14                  (C/C++)
│               ├── python:3.12             (Python)
│               └── node:22                 (JavaScript)
├── /var/lib/codearena/workspaces/
└── 2 GB swap
```

Total RAM: ~3-4 GB. Comfortably within the 24 GB free allocation.

---

## Prerequisites

- Oracle Cloud account ([oracle.com/cloud/free](https://www.oracle.com/cloud/free))
- SSH key pair (`ssh-keygen -t ed25519` if you don't have one)
- Git (to clone the project)

---

## Step 1: Create the ARM VM

1. Log in to [Oracle Cloud Console](https://cloud.oracle.com)
2. Navigate to **Compute > Instances > Create Instance**
3. Configure:
   - **Name**: `codearena`
   - **Image**: Ubuntu 22.04 (or Ubuntu 24.04)
   - **Shape**: `VM.Standard.A1.Flex`
     - OCPUs: **2** (or 4 if available)
     - Memory: **12 GB** (or 24 GB)
   - **SSH Keys**: Paste your public key
   - **Virtual Cloud Network**: Create new VCN (or use existing)
   - **Subnet**: Public
   - **Public IP**: Assign ephemeral public IP
4. Click **Create** and wait 2-3 minutes for the instance to start
5. Note the **Public IP Address** (e.g., `129.146.xxx.xxx`)

---

## Step 2: Open Port 8081

1. Go to **Networking > Virtual Cloud Networks > [your VCN]**
2. Click **Default Security List** (or your custom security list)
3. Add **Ingress Rule**:
   - **Source CIDR**: `0.0.0.0/0`
   - **Destination Port**: `8081`
   - **Protocol**: TCP
4. Click **Add Ingress Rules**

---

## Step 3: SSH Into the VM

```bash
ssh -i ~/.ssh/id_ed25519 ubuntu@<YOUR_PUBLIC_IP>
```

---

## Step 4: Bootstrap the VM

```bash
# Download the setup script (or copy it from your local machine)
cat > setup-vm.sh << 'SETUP'
# ... paste contents of setup-vm.sh here ...
SETUP

chmod +x setup-vm.sh
./setup-vm.sh
```

Or copy from your local machine:
```bash
scp setup-vm.sh ubuntu@<YOUR_PUBLIC_IP>:~/
ssh ubuntu@<YOUR_PUBLIC_IP> "./setup-vm.sh"
```

After setup completes, **log out and back in** (for docker group to take effect):
```bash
exit
ssh -i ~/.ssh/id_ed25519 ubuntu@<YOUR_PUBLIC_IP>
```

---

## Step 5: Deploy

### Option A: One-command deploy (local → remote)

From your **local machine** (where the code lives):

```bash
./deploy.sh ubuntu@<YOUR_PUBLIC_IP>
```

This will:
1. Build the JAR locally
2. Rsync the project to the VM
3. Generate secrets in `.env`
4. Run `docker compose up -d --build`

### Option B: Deploy directly on the VM

```bash
# Clone or copy the project to the VM
git clone <your-repo-url> /opt/codearena
# OR
scp -r . ubuntu@<YOUR_PUBLIC_IP>:/opt/codearena/

cd /opt/codearena

# Generate secrets
cp .env.example .env
sed -i "s/^RUNNER_TOKEN=.*/RUNNER_TOKEN=$(openssl rand -hex 32)/" .env
sed -i "s/^DB_PASSWORD=.*/DB_PASSWORD=$(openssl rand -hex 16)/" .env

# Deploy
docker compose up -d --build
```

---

## Step 6: Verify

```bash
# Health check
curl http://<YOUR_PUBLIC_IP>:8081/actuator/health

# Expected output:
# {"status":"UP","components":{"db":{"status":"UP"},...}}
```

---

## Step 7: Expose to the Internet (Optional)

By default, the service is accessible via `http://<YOUR_PUBLIC_IP>:8081`.

### Option A: Use Caddy (auto-HTTPS)

```bash
# Install Caddy
sudo apt-get install -y -qq caddy

# Configure reverse proxy
sudo tee /etc/caddy/Caddyfile >/dev/null << 'EOF'
codearena.yourdomain.com {
    reverse_proxy localhost:8081
}
EOF

# Restart Caddy
sudo systemctl restart caddy
```

### Option B: Use Nginx (manual HTTPS)

```bash
sudo apt-get install -y -qq nginx certbot python3-certbot-nginx
# Configure /etc/nginx/sites-available/codearena
# Then: sudo certbot --nginx -d codearena.yourdomain.com
```

---

## Configuration

All configuration is in `.env` on the VM:

| Variable | Description | Default |
|----------|-------------|---------|
| `RUNNER_TOKEN` | Secret token for API access | (generated) |
| `DB_PASSWORD` | MySQL root password | `codearena_root` |
| `JAVA_OPTS` | JVM heap settings | `-Xmx512m` |

To change settings:
```bash
cd /opt/codearena
nano .env
docker compose restart runner
```

---

## Monitoring

### Check service status
```bash
docker compose ps
docker compose logs -f runner
```

### Check resource usage
```bash
# Memory
free -h

# CPU
top -bn1 | head -5

# Docker containers
docker stats --no-stream

# Disk
df -h /
```

### Check MySQL
```bash
docker compose exec mysql mysql -uroot -p"$DB_PASSWORD" code_arena -e "SELECT COUNT(*) FROM execution_log;"
```

---

## Troubleshooting

### "Cannot connect to Docker daemon"
```bash
sudo systemctl status docker
sudo systemctl restart docker
groups $USER  # should include "docker"
# Log out and back in if "docker" is missing
```

### "Out of memory" during build
```bash
# The build uses ~2 GB RAM. Add swap if not already done:
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

### "No space left on device"
```bash
# Clean Docker images
docker system prune -af
# Check disk
df -h /
```

### Health check returns DOWN
```bash
docker compose logs runner | tail -30
# Common issues:
# - Wrong RUNNER_TOKEN
# - MySQL not ready (wait 30s and retry)
# - Port conflict (check nothing else uses 8081)
```

### Out of capacity for ARM shape
If Oracle Cloud says "Out of capacity for VM.Standard.A1.Flex":
1. Try a different **Availability Domain** (there are usually 3)
2. Try a different **Region** (Tokyo, Seoul, etc.)
3. Try again later — capacity is added periodically

---

## Updating

```bash
ssh ubuntu@<YOUR_PUBLIC_IP>
cd /opt/codearena

# Pull latest code
git pull

# Rebuild and restart
docker compose up -d --build

# Verify
curl http://localhost:8081/actuator/health
```

---

## Cleanup (to delete everything)

```bash
ssh ubuntu@<YOUR_PUBLIC_IP>
cd /opt/codearena

# Stop all containers
docker compose down -v  # -v removes volumes (MySQL data!)

# Remove the project
cd /
sudo rm -rf /opt/codearena

# Then terminate the instance in Oracle Cloud Console
```

---

## Cost Breakdown

| Resource | Usage | Cost |
|----------|-------|------|
| ARM VM (2 OCPU, 12 GB) | Always Free | $0 |
| 200 GB boot volume | Always Free | $0 |
| Outbound traffic (10 TB) | Always Free | $0 |
| **Total** | | **$0/month** |

> As long as you stay within Always Free limits, this deployment costs nothing.
> Always Free resources do not expire and are not subject to the 30-day trial.
