# TomTune

- Lire `SPEC.md` pour le produit et le fichier du lot demandé pour le périmètre de la tâche. Pour le lot 0, lire `docs/lot0/LOT-0.md`.
- Utiliser Kotlin Multiplatform et Compose. Garder le calcul audio partagé testable avec une source PCM injectée. Isoler le micro et le rendu propres à chaque plateforme.
- Distinguer la cible du fût entier, les fréquences près des vis et les mesures. Modifier une référence enregistrée uniquement sur action explicite de l’utilisateur.
- Utiliser des ressources de texte FR, EN, ES et DE. Le stockage des hauteurs et les identifiants sont indépendants de la langue.
- Donner à chaque sous-agent un périmètre et des fichiers distincts. L’agent principal est responsable du build, de l’intégration et de la vérification des parcours.
- Consigner commandes et résultats. Distinguer tests sur signaux, simulations et essais réels sur téléphone. Traiter le signal micro en mémoire sur l’appareil.
