# Démarrer TomTune dans Codex

Ouvrir un clone de calabo-Act21/TomTune dans Codex. Ajouter les fichiers de ce dossier à la racine en conservant leur arborescence, y compris le dossier caché .codex. Créer un premier commit de documentation. Cette base contient les décisions du lot 0, les fichiers de consignes et les ressources. La spec et les critères de la tâche font foi. Le filaire illustre l’interaction et l’image le rendu attendu.

Android se vérifie avec le SDK Android et Gradle. Prévoir un Mac avec Xcode pour la compilation iOS. Les mesures du micro et de la fluidité exigent les téléphones physiques. Sur un environnement distant, documenter uniquement les vérifications réellement exécutées et compléter les autres depuis la machine connectée aux appareils.

## Prompt à lancer sur le dépôt

> Travaille sur le lot 0 uniquement. Lis AGENTS.md, SPEC.md et docs/lot0/LOT-0.md. Confirme que tu peux ouvrir le filaire et l’image, et vérifie les outils Android et iOS présents. Pars sur Kotlin Multiplatform avec Compose. Fixe les interfaces communes de capture et de scène. Lance les sous-agents audio et rendu_3d en parallèle avec des fichiers distincts, puis validation en lecture seule sur le diff intégré. Ouvre une branche dédiée et livre l’écran de faisabilité, les résultats des tests et les essais restants sur téléphone. Fais une PR brouillon si la session a accès au dépôt distant.

Pour les lots suivants, une tâche et un diff révisable par lot. Si une décision change en conversation, reporter la décision dans le fichier de spec ou de lot avant d’engager le code correspondant.
