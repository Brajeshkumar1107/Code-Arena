#!/usr/bin/env bash
# deploy.sh — Deploy CodeArena Runner to the local machine (or an Oracle Cloud VM).
#
# Usage:
#   Local:   ./deploy.sh
#   Remote:  ./deploy.sh <user>@<host>   # SSHs in, transfers project, deploys
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_NAME="codearena"
DEPLOY_DIR="/opt/codearena"

# ---------- Helpers ----------
red()    { printf '\033[1;31m%s\033[0m\n' "$*"; }
green()  { printf '\033[1;32m%s\033[0m\n' "$*"; }
yellow() { printf '\033[1;33m%s\033[0m\n' "$*"; }

# ---------- Remote deploy via SSH ----------
if [ "${1:-}" != "" ]; then
    REMOTE="$1"
    echo "==> Deploying to ${REMOTE}:${DEPLOY_DIR}"

    echo "[1/4] Transferring project..."
    rsync -az --delete \
        --exclude='.git' \
        --exclude='target' \
        --exclude='.mvn' \
        --exclude='node_modules' \
        "${SCRIPT_DIR}/" "${REMOTE}:${DEPLOY_DIR}/"

    echo "[2/4] Setting up environment..."
    ssh "${REMOTE}" "chmod +x ${DEPLOY_DIR}/setup-vm.sh && \
                      mkdir -p ${DEPLOY_DIR} && \
                      cd ${DEPLOY_DIR} && \
                      [ -f .env ] || cp .env.example .env"

    echo "[3/4] Generating secrets (if .env is empty)..."
    ssh "${REMOTE}" "cd ${DEPLOY_DIR} && \
                      TOKEN=\$(openssl rand -hex 32) && \
                      PASS=\$(openssl rand -hex 16) && \
                      sed -i \"s/^RUNNER_TOKEN=.*/RUNNER_TOKEN=\${TOKEN}/\" .env && \
                      sed -i \"s/^DB_PASSWORD=.*/DB_PASSWORD=\${PASS}/\" .env && \
                      echo '  Secrets written to .env'"

    echo "[4/4] Starting services..."
    ssh "${REMOTE}" "cd ${DEPLOY_DIR} && docker compose up -d --build"

    echo ""
    green "==> Deployed to ${REMOTE}"
    echo "    Health: curl http://$(echo "${REMOTE}" | cut -d@ -f2):8081/actuator/health"
    exit 0
fi

# ---------- Local deploy ----------
echo "==> Local deploy"

echo "[1/4] Building JAR..."
cd "${SCRIPT_DIR}"
mvn -B -DskipTests clean package >/dev/null
JAR=$(ls target/CodeArena-*.jar 2>/dev/null | head -1)
if [ -z "${JAR}" ]; then
    red "ERROR: JAR not found in target/"
    exit 1
fi
green "  Built: $(basename "${JAR}")"

echo "[2/4] Preparing workspace..."
mkdir -p /var/lib/codearena/workspaces
chown 1000:1000 /var/lib/codearena/workspaces 2>/dev/null || true

echo "[3/4] Generating .env (if needed)..."
cd "${SCRIPT_DIR}"
if [ ! -f .env ] || grep -q "^RUNNER_TOKEN=$" .env; then
    cp -n .env.example .env 2>/dev/null || true
    TOKEN=$(openssl rand -hex 32)
    PASS=$(openssl rand -hex 16)
    sed -i "s/^RUNNER_TOKEN=.*/RUNNER_TOKEN=${TOKEN}/" .env
    sed -i "s/^DB_PASSWORD=.*/DB_PASSWORD=${PASS}/" .env
    green "  .env generated with random secrets"
else
    green "  .env already configured"
fi

echo "[4/4] Starting services..."
cd "${SCRIPT_DIR}"
docker compose up -d --build

echo ""
green "==> Local deploy complete"
echo "    Health: curl http://localhost:8081/actuator/health"
echo "    Logs:   docker compose logs -f runner"
