#!/usr/bin/env bash
# Instala LevelUp Store (API + web) en una EC2 Ubuntu o Amazon Linux 2023.
# Uso: bash instalar-ec2.sh
set -euo pipefail

REPO_API="${REPO_API:-https://github.com/JonaSlack/levelup-store-backend.git}"
REPO_WEB="${REPO_WEB:-https://github.com/JonaSlack/levelup-gamer-store.git}"
DB_PASS="${DB_PASS:-$(openssl rand -hex 12)}"
USUARIO="$(whoami)"
BASE="$HOME/levelup"

echo "==> Memoria de intercambio"
if ! swapon --show | grep -q swapfile; then
  sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
  sudo mkswap /swapfile && sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab > /dev/null
fi

echo "==> Paquetes"
if command -v apt-get > /dev/null; then
  sudo apt-get update -y
  sudo apt-get install -y openjdk-21-jdk maven mysql-server git curl
  SERVICIO_DB=mysql
else
  sudo dnf install -y java-21-amazon-corretto-devel maven mariadb105-server git
  SERVICIO_DB=mariadb
fi
sudo systemctl enable --now "$SERVICIO_DB"

if ! command -v npm > /dev/null; then
  echo "==> Node.js 22"
  if command -v apt-get > /dev/null; then
    curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
    sudo apt-get install -y nodejs
  else
    sudo dnf install -y nodejs22 || sudo dnf install -y nodejs
  fi
fi
NPM="$(command -v npm)"
NODE_DIR="$(dirname "$(command -v node)")"

echo "==> Base de datos"
sudo mysql -e "CREATE DATABASE IF NOT EXISTS levelup_store CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'levelup'@'localhost' IDENTIFIED BY '${DB_PASS}';
ALTER USER 'levelup'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON levelup_store.* TO 'levelup'@'localhost';
FLUSH PRIVILEGES;"

echo "==> Código"
mkdir -p "$BASE"
for par in "api|$REPO_API" "web|$REPO_WEB"; do
  dir="$BASE/${par%%|*}"; url="${par#*|}"
  if [ -d "$dir/.git" ]; then git -C "$dir" pull --ff-only; else git clone "$url" "$dir"; fi
done

echo "==> Backend"
# maven puede instalar otro JDK como predeterminado; se fuerza el 21
JAVA21="$(ls -d /usr/lib/jvm/*21* 2>/dev/null | grep -v jre | head -1)"
if [ -z "$JAVA21" ] || [ ! -x "$JAVA21/bin/javac" ]; then
  echo "No se encontró el JDK 21 en /usr/lib/jvm" >&2
  exit 1
fi
export JAVA_HOME="$JAVA21"
(cd "$BASE/api" && mvn -B -q -DskipTests package)
sudo mkdir -p /opt/levelup
sudo cp "$BASE"/api/target/levelup-store-backend-*.jar /opt/levelup/app.jar
if [ ! -f /etc/levelup.env ]; then
  sudo tee /etc/levelup.env > /dev/null <<ENV
DB_URL=jdbc:mysql://localhost:3306/levelup_store?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Santiago
DB_USER=levelup
DB_PASSWORD=${DB_PASS}
JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
ENV
  sudo chmod 600 /etc/levelup.env
else
  sudo sed -i "s/^DB_PASSWORD=.*/DB_PASSWORD=${DB_PASS}/" /etc/levelup.env
fi
JAVA="$JAVA21/bin/java"
sudo tee /etc/systemd/system/levelup-api.service > /dev/null <<UNIT
[Unit]
Description=LevelUp Store API
After=network.target ${SERVICIO_DB}.service

[Service]
User=${USUARIO}
EnvironmentFile=/etc/levelup.env
ExecStart=${JAVA} -Xmx384m -jar /opt/levelup/app.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT

echo "==> Frontend"
(cd "$BASE/web" && "$NPM" ci && "$NPM" run build)
sudo tee /etc/systemd/system/levelup-web.service > /dev/null <<UNIT
[Unit]
Description=LevelUp Store Web
After=network.target levelup-api.service

[Service]
User=${USUARIO}
WorkingDirectory=${BASE}/web
Environment=PATH=${NODE_DIR}:/usr/bin:/bin
ExecStart=${NPM} run preview -- --host 0.0.0.0 --port 5173
Restart=always

[Install]
WantedBy=multi-user.target
UNIT

# detiene un vite que haya quedado corriendo a mano en el 5173
pkill -f "vite" 2>/dev/null || true

sudo systemctl daemon-reload
sudo systemctl enable levelup-api levelup-web
sudo systemctl restart levelup-api levelup-web

echo "==> Esperando a la API"
for i in $(seq 1 60); do
  curl -fs localhost:8080/api/productos > /dev/null && break
  sleep 3
done

IP="$(curl -fs --max-time 3 http://checkip.amazonaws.com || echo IP_PUBLICA)"
echo
echo "Listo:"
echo "  Tienda:  http://${IP}:5173"
echo "  Swagger: http://${IP}:5173/swagger-ui.html"
echo "  Logs:    sudo journalctl -u levelup-api -f"
echo "Recuerda abrir el puerto 5173 en el Security Group."
