# TomTune

- Lire `SPEC.md` et le fichier du lot demandé. Pour le lot 1, lire `docs/lot1/LOT-1.md` et comparer les écrans au filaire et au parcours annoté dans `docs/maquettes/`.
- Utiliser Kotlin Multiplatform et Compose. Garder le calcul audio partagé testable avec une source PCM injectée. Isoler le micro et les intégrations propres à chaque plateforme.
- Distinguer la cible du fût entier, les fréquences près des vis et les mesures. Modifier une référence enregistrée uniquement sur action explicite de l’utilisateur.
- Utiliser les ressources FR, EN, ES et DE. Le stockage des hauteurs et les identifiants sont indépendants de la langue.
- Pour le lot 1, cibler Android et suivre la boucle de validation légère du brief. Un agent par défaut. Faire le build debug en fin d’itération et conserver les tests existants. Effectuer les vérifications liées aux fichiers touchés. Réserver les sous-agents à des périmètres indépendants ou à une demande explicite.
- Consigner les commandes et résultats. Distinguer signal injecté, simulation et essai physique. Traiter le PCM en mémoire sur l’appareil.
