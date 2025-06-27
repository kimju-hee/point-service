#!/bin/bash
set -e

echo "[0] 유틸리티 설치 (기본 툴)"
sudo apt-get update
sudo apt-get install -y curl apt-transport-https ca-certificates gnupg lsb-release

echo "[1] httpie 설치"
pip install --upgrade pip
pip install httpie

echo "[2] Azure CLI 설치"
curl -sL https://packages.microsoft.com/keys/microsoft.asc | \
  gpg --dearmor | \
  sudo tee /etc/apt/trusted.gpg.d/microsoft.gpg > /dev/null

echo "deb [arch=amd64] https://packages.microsoft.com/repos/azure-cli/ jammy main" | \
  sudo tee /etc/apt/sources.list.d/azure-cli.list

sudo apt-get update
sudo apt-get install -y azure-cli

echo "[3] Helm 설치"
curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash

echo "모든 초기 설정 완료!"
