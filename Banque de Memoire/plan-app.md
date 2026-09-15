# Plan de Mise en Œuvre : Application Mobile Coursier (Kotlin Natif)

**ATTENTION POUR L'AGENT IA :** L'objectif critique est le maintien du GPS en arrière-plan. Utiliser impérativement Kotlin et les API natives d'Android. Pas de modules de paiement.

---

## Phase 1 : Initialisation et Authentification
### Étape 1.1 : Setup du projet et UI de base
*   **Instruction :** Créer un projet Android Studio (Kotlin). Mettre en place l'architecture MVVM, configurer Retrofit pour l'API et créer l'écran de Login.
*   **Test de validation :** Compiler l'APK, l'installer sur un téléphone physique. L'écran de connexion doit s'afficher.

### Étape 1.2 : Connexion à l'API
*   **Instruction :** Implémenter l'appel réseau vers `/api/auth/login`. Stocker le token JWT de manière sécurisée (EncryptedSharedPreferences).
*   **Test de validation :** Saisir des identifiants valides. L'application doit réussir la connexion et rediriger vers l'écran principal.

---

## Phase 2 : Liste des Courses et Navigation
### Étape 2.1 : Récupération des missions
*   **Instruction :** Créer un écran listant les courses assignées au coursier connecté (appel à l'API backend).
*   **Test de validation :** L'écran doit afficher au moins une course préalablement assignée via le dashboard web.

### Étape 2.2 : Redirection GPS (Intent)
*   **Instruction :** Sur le détail d'une course, ajouter un bouton "Naviguer". Ce bouton doit déclencher un Intent pour ouvrir Google Maps avec les coordonnées de destination.
*   **Test de validation :** Cliquer sur "Naviguer". L'application Google Maps doit s'ouvrir avec l'itinéraire pré-chargé.

---

## Phase 3 : Le Moteur GPS (Foreground Service)
### Étape 3.1 : Implémentation du Foreground Service
*   **Instruction :** Créer un service Android en premier plan (`Foreground Service`) avec une notification persistante informant que le tracking est actif.
*   **Test de validation :** Lancer le service. Vérifier que la notification apparaît et qu'elle ne peut pas être balayée (swipe) par l'utilisateur.

### Étape 3.2 : Captation et Envoi des positions
*   **Instruction :** Configurer `FusedLocationProviderClient` pour récupérer la position toutes les X secondes. Créer une coroutine pour envoyer ces coordonnées à l'API (`/api/gps/update`).
*   **Test de validation :** Lancer le tracking, verrouiller l'écran du smartphone, et marcher en extérieur pendant 3 minutes. Vérifier dans la base de données backend que les points ont été enregistrés sans trou.

### Étape 3.3 : Gestion des permissions et Batterie
*   **Instruction :** Implémenter la demande des permissions `ACCESS_FINE_LOCATION` et `ACCESS_BACKGROUND_LOCATION`. Ajouter le prompt invitant à désactiver l'optimisation de batterie pour l'app.
*   **Test de validation :** Installer l'app à neuf. Vérifier que le tunnel de permissions bloque l'accès tant que l'utilisateur n'a pas tout accepté.

---

## Phase 4 : Validation de Remise OTP
### Étape 4.1 : Interface de Saisie OTP
*   **Instruction :** Dans l'écran de détail d'une course, ajouter le champ de saisie à 4 chiffres pour l'OTP et le bouton de validation.
*   **Test de validation :** L'interface doit empêcher la saisie de texte (clavier numérique uniquement) et limiter la longueur à 4 caractères.

### Étape 4.2 : Consommation de l'API de Validation
*   **Instruction :** Connecter le bouton à l'endpoint `/api/deliveries/validate`. Gérer les retours (Succès -> changer statut localement, Échec -> afficher message d'erreur).
*   **Test de validation :** Saisir un faux code : l'application doit afficher une erreur. Saisir le vrai code : l'application doit afficher un succès, et la course doit disparaître des tâches en cours.
