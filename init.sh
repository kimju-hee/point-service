#!/bin/bash
set -e

echo "[0] 유틸리티 설치"
sudo apt-get update
sudo apt-get install -y net-tools iputils-ping apt-transport-https ca-certificates curl gnupg lsb-release

echo "[1] httpie 설치"
pip install httpie

echo "[2] Azure CLI 설치"
curl -sL https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/microsoft.gpg > /dev/null
echo "deb [arch=amd64] https://packages.microsoft.com/repos/azure-cli/ jammy main" | sudo tee /etc/apt/sources.list.d/azure-cli.list
sudo apt update
sudo apt install -y azure-cli

echo "[3] Helm 설치"
curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash

echo "[4] kubectl 설치"
KUBECTL_VERSION=$(curl -Ls https://dl.k8s.io/release/stable.txt)
curl -LO "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

echo "[5] Kafka 설치 (Helm)"
helm repo add bitnami https://charts.bitnami.com/bitnami || echo "bitnami repo 이미 존재"
helm repo update
helm install my-kafka bitnami/kafka --version 26.2.0 --wait || echo "⚠️ Kafka 설치 중 오류 발생 또는 이미 설치됨"

echo "모든 초기 설정 완료!"
