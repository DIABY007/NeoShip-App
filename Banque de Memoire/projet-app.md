# Document de Conception : Application Mobile Coursier (Natif)

## 1. Rôle du Projet
Ce dépôt est l'outil de terrain. Ses deux seules raisons d'être sont de remonter la position GPS du coursier sans interruption et de sécuriser la remise du colis via le protocole OTP.

## 2. Périmètre Actif (Sprint Actuel)

### 2.1. Le Moteur GPS (Priorité Critique)
*   **Tracking continu :** L'application doit maintenir un "Foreground Service" actif pour capturer la position et l'envoyer à l'API centrale à intervalles réguliers.
*   **Résilience :** Contournement obligatoire de l'optimisation de batterie (Doze Mode) d'Android. Si le tracking coupe quand le téléphone est en veille, l'application est considérée comme défectueuse.

### 2.2. Interface Coursier
*   **Authentification :** Écran de connexion lié à l'API.
*   **File d'attente :** Affichage des courses assignées avec les adresses de départ et de destination.
*   **Navigation :** Bouton de redirection (Intent) vers Google Maps ou Waze pour guider le coursier.

### 2.3. Validation de Remise ("Pas de code, pas de colis")
*   **Saisie OTP :** Écran de livraison incluant un champ pour le code à 4 chiffres.
*   **Règle absolue :** La validation interroge l'API centrale. En cas de code incorrect, la course ne peut pas être clôturée. Il n'y a aucun bouton de contournement ("bypass") pour le coursier.

## 3. Périmètre Gelé (Hors Scope)
*   **Encaissement terrain :** Aucune gestion de la monnaie, de Mobile Money ou de scan de QR code de paiement n'est à prévoir dans cette version.
