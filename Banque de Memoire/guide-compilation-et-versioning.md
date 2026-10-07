# Guide de compilation et gestion des versions — Licopress Courier

## 1. Compilation de l'APK

### Prérequis

- **Android SDK** installé (déjà fait sur le serveur)
- **Java 17+** avec Gradle 8.5
- Projet Android (Kotlin + Jetpack Compose)

### Compilation

```bash
# Depuis la racine du projet
cd ~/Desktop/NeoShip\ APP

# Compiler l'APK de debug
export ANDROID_HOME=/home/diaby007/android-sdk
./gradlew assembleDebug

# L'APK est généré ici :
# app/build/outputs/apk/debug/app-debug.apk
```

### Installer sur le téléphone

```bash
# Méthode 1 : via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Méthode 2 : copier le fichier et l'installer manuellement
cp app/build/outputs/apk/debug/app-debug.apk ~/neoship.apk
# Puis transférer via USB/WhatsApp/email et ouvrir le fichier .apk
```

---

## 2. Gestion des versions

### À chaque mise à jour, suivre ces étapes :

### Étape 1 — Builder la nouvelle version de l'APK

```bash
# 1. Incrémenter le numéro de version
vim app/build.gradle.kts
# Modifier : versionCode = 8 → versionCode = 9 (incrémenter de 1)
```

```kts
// build.gradle.kts (extrait)
versionCode = 9   // ← incrémenter ici
versionName = "1.0.0"
```

### Étape 2 — Compiler

```bash
./gradlew assembleDebug
```

### Étape 3 — Copier l'APK vers le serveur web

```bash
cp app/build/outputs/apk/debug/app-debug.apk \
   /home/diaby007/Desktop/NeoShip\ Web/neoship-web/public/neoship.apk
```

### Étape 4 — Publier la mise à jour sur le dashboard

1. Se connecter sur **https://licopress.space**
2. Aller dans **Paramètres** → section **Mise à jour de l'application**
3. L'URL de l'APK est déjà pré-remplie : `http://licopress.space/neoship.apk`
4. Ajouter un message de version (optionnel) :
   ```
   - Version 9 : correction de bugs
   - Version 8 : nouveau logo, rebranding Licopress
   ```
5. Cliquer sur **Publier une mise à jour**

### Étape 5 — Pousser sur GitHub

```bash
git add -A
git commit -m "Version 9 : description des changements"
git push
```

---

## 3. Schéma du cycle de version

```
App (versionCode = 8)
         │
         ├── Backend : latestVersion = 7 (pas de mise à jour dispo)
         │
         │   Dashboard admin → Publier une mise à jour
         │       │
         │       ├── latestVersion = 8
         │       │
         │       └── L'app détecte 8 > 8 ? Non → rien
         │
         │   (quand une vraie mise à jour est prête)
         │
         ├── versionCode = 9 (build)
         ├── Backend → Publier → latestVersion = 9
         │
         └── L'app détecte 9 > 8 → bouton "Mettre à jour"
```

---

## 4. Fichiers importants

| Fichier | Rôle |
|---|---|
| `app/build.gradle.kts` | versionCode, dépendances, config build |
| `app/src/main/AndroidManifest.xml` | Permissions Android, icônes, services |
| `app/src/main/res/values/strings.xml` | Textes affichés dans l'app (nom, notifications) |
| `Banque de Memoire/stack_mobile_app.md` | Stack technique du projet |
| `local.properties` | Configuration locale (API URL, SDK path) |

---

## 5. Vérification après build

```bash
# Tester l'API
curl -s http://licopress.space/api/app/version

# Vérifier que l'APK est accessible
curl -sI http://licopress.space/neoship.apk | head -3

# Vérifier le nombre de versions stockées
PGPASSWORD=neoship_password \
  psql -h localhost -p 5433 -U neoship -d neoship_db \
  -c "SELECT key, value FROM config WHERE key = 'latest_version';"
```