#!/usr/bin/env bash
# deploy-aws.sh — Deploy CodeArena Runner to an AWS t4g.small VM (2 GB RAM).
#
# This uses the lean docker-compose.aws.yml (MySQL + Runner only, Kafka disabled).
#
# Usage:
#   ./deploy-aws.sh <user>@<host>   # e.g. ./deploy-aws.sh ubuntu@129.146.xxx.xxx
#
# Prerequisites on the VM:
#   - Ubuntu 22.04+ ARM (t4g.small)
#   - Docker installed (run setup-vm.sh first)
#   - User in docker group (log out and back in after setup)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEPLOY_DIR="/opt/codearena"

# ---------- Helpers ----------
red()    { printf '\033[1;31m%s\033[0m\n' "$*"; }
green()  { printf '\033[1;32m%s\033[0m\n' "$*"; }
yellow() { printf '\033[1;33m%s\033[0m\n' "$*"; }

if [ "${1:-}" = "" ]; then
    red "Usage: $0 <user>@<host>"
    red "Example: $0 ubuntu@129.146.123.45"
    exit 1
fi

REMOTE="$1"
HOST=$(echo "${REMOTE}" | cut -d@ -f2)

echo "==> Deploying CodeArena to ${REMOTE}:${DEPLOY_DIR}"
echo "    (AWS lean mode: MySQL + Runner, Kafka disabled)"
echo ""

echo "[1/5] Transferring project..."
rsync -az --delete \
    --exclude='.git' \
    --exclude='target' \
    --exclude='.mvn' \
    --exclude='node_modules' \
    --exclude='docker-compose.yml' \
    "${SCRIPT_DIR}/" "${REMOTE}:${DEPLOY_DIR}/"
green "  Project transferred"

echo "[2/5] Setting up workspace directory..."
ssh "${REMOTE}" "sudo mkdir -p /var/lib/codearena/workspaces && \
                  sudo chown 1000:1000 /var/lib/codearena/workspaces"
green "  Workspace ready"

echo "[3/5] Generating secrets..."
ssh "${REMOTE}" "cd ${DEPLOY_DIR} && \
                  [ -f .env ] || cp .env.example .env && \
                  TOKEN=\$(openssl rand -hex 32) && \
                  PASS=\$(openssl rand -hex 16) && \
                  sed -i \"s/^RUNNER_TOKEN=.*/RUNNER_TOKEN=\${TOKEN}/\" .env && \
                  sed -i \"s/^DB_PASSWORD=.*/DB_PASSWORD=\${PASS}/\" .env && \
                  echo '  Secrets written to .env'"
green "  Secrets generated"

echo "[4/5] Pre-pulling language sandbox images (this may take a few minutes)..."
ssh "${REMOTE}" "docker pull eclipse-temurin:21-jdk && \
                  docker pull gcc:14 && \
                  docker pull python:3.12 && \
                  docker pull node:22"
green "  Sandbox images ready"

echo "[5/5] Building and starting services..."
ssh "${REMOTE}" "cd ${DEPLOY_DIR} && docker compose -f docker-compose.aws.yml up -d --build"
green "  Services started"

echo ""
green "==> Deployed to ${REMOTE}"
echo ""
echo "    Health check:"
echo "      curl http://${HOST}:8081/actuator/health"
echo ""
echo "    Execute code:"
echo "      curl -X POST http://${HOST}:8081/api/v1/runner/execute \\"
echo "        -H 'Content-Type: application/json' \\"
echo "        -d '{\"mode\":\"RUN\",\"language\":\"PYTHON\",\"sourceCode\":\"print(42)\",\"testCases\":[{\"input\":\"\"}]}'"
echo ""
echo "    Logs:"
echo "      ssh ${REMOTE} 'cd ${DEPLOY_DIR} && docker compose -f docker-compose.aws.yml logs -f runner'"
