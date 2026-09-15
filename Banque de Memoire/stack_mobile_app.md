# Stack Technique : Application Mobile (Coursiers)

**ATTENTION POUR L'AGENT IA :** Cette application a une seule fonction critique : le tracking GPS en continu. Les frameworks hybrides (React Native, Flutter, PWA) sont bannis pour ce projet en raison des limitations de l'OS Android sur les tâches de fond (Doze Mode). 

## 1. Vue d'ensemble de l'Architecture Mobile
*   **Plateforme cible :** Android (Natif) exclusivement (fichier `.apk`).
*   **Langage :** Kotlin.
*   **Architecture logicielle :** MVVM (Model-View-ViewModel) pour une séparation claire de la logique et de l'interface.

## 2. Contrainte Majeure : Tracking GPS (Foreground Service)
*   **Technologie :** Android Foreground Service.
*   **Localisation :** FusedLocationProviderClient (Google Play Services) ou LocationManager natif.
*   **Exigences strictes :**
    *   Le service doit afficher une notification persistante indiquant au coursier que son trajet est enregistré.
    *   Le code doit intégrer les requêtes de permissions spécifiques : `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, et `FOREGROUND_SERVICE`.
    *   Implémentation d'un mécanisme pour contourner l'optimisation de la batterie (demander à l'utilisateur d'ignorer les optimisations de batterie pour cette app).

## 3. Communication Réseau & API
*   **Client HTTP :** Retrofit 2 + OkHttp.
    *   *Rôle :* Consommer l'API Next.js (Authentification, récupération des courses, envoi des pings GPS, validation de l'OTP).
*   **Sérialisation :** Gson ou Moshi.
*   **Gestion de l'asynchronisme :** Kotlin Coroutines. Envoi des positions GPS dans une coroutine non bloquante.

## 4. Interface Utilisateur (UI)
*   **Toolkit :** Jetpack Compose avec Material 3 strict. Interdiction d'utiliser des librairies UI tierces.
*   **Écrans clés :**
    *   Login.
    *   Liste des livraisons assignées.
    *   Détail d'une livraison (incluant le champ de saisie du code OTP).
*   **Règle métier UI :** Le bouton "Valider la course" doit être inopérant (ou grisé) tant que l'OTP n'est pas saisi et validé par le serveur.

## 5. Cartographie (Optionnel sur l'app)
*   *Note :* Le coursier n'a pas strictement besoin d'une carte complexe dans son app s'il connaît la ville, mais s'il faut ouvrir l'itinéraire, utiliser un Intent implicite pour ouvrir Google Maps via URI (`google.navigation:q=latitude,longitude`) est la solution la plus légère et rapide.
