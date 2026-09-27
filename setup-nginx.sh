#!/bin/bash
# À exécuter : bash setup-nginx.sh
# (le mot de passe sudo sera demandé)

set -e

echo "📦 Création de la config Nginx..."

sudo tee /etc/nginx/sites-available/licopress.space > /dev/null << 'EOF'
server {
    listen 80;
    listen [::]:80;
    server_name licopress.space www.licopress.space;
    client_max_body_size 50M;

    location / {
        proxy_pass http://127.0.0.1:3004;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
EOF

echo "✅ Config créée"

sudo ln -sf /etc/nginx/sites-available/licopress.space /etc/nginx/sites-enabled/
echo "✅ Site activé"

sudo nginx -t && sudo systemctl reload nginx
echo "✅ Nginx rechargé"

echo ""
echo "🌐 http://licopress.space devrait maintenant fonctionner"