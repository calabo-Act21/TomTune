# Prompt Codex — TomTune, lot 1

Travaille dans `calabo-Act21/TomTune`. Vérifie la branche et le commit de départ, puis la présence du code du lot 0 et de la passation du lot 1. Pars d’une branche qui contient les deux. N’initialise pas un second projet.

Lis `AGENTS.md`, `SPEC.md`, `docs/lot1/LOT-1.md`, `docs/maquettes/PARCOURS.md` et `docs/visuels/INDEX.md`. Ouvre le filaire et compare l’accueil, l’aperçu et la configuration à ce parcours. Suis les règles récentes du parcours annoté lorsque le filaire date d’avant une décision.

Implémente uniquement le lot 1 Android dans Kotlin Multiplatform et Compose. Utilise les PNG nommés dans l’index, les coordonnées normalisées des fûts et le contrat de stockage du brief. Vérifie que chaque kit s’ouvre, s’enregistre et reste sélectionné après relance hors ligne. Préserve le code audio du lot 0 et les cibles iOS.

Travaille avec un seul agent. Fais un build Android debug en fin d’itération et relance uniquement pour corriger un échec concret. Réutilise les tests existants quand ils concernent le diff. N’ouvre pas de PR et ne lance pas la CI. Fournis le chemin de l’APK, les commandes et résultats et le tableau A1 à A9 pour que je valide le parcours sur mon Android. Si le build n’a pas pu tourner, précise la cause et la commande à reproduire. Termine avec un diff révisable dans une branche dédiée.
