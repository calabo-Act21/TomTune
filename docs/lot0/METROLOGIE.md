# Diagnostic des essais sur appareil

L’APK **debug** Android journalise uniquement des métadonnées `TomTuneTrace` : identifiant d’exécution, session, frappe, événement, Hz/rejet, horodatage monotone et tick de l’horloge Compose. Aucun échantillon PCM n’est écrit. Le signal audio reste en mémoire. Ce diagnostic ne change ni référence ni mesure enregistrée dans le produit.

- `CLOCK` : origine des frames PCM estimée avec `AudioRecord.getTimestamp(...TIMEBASE_MONOTONIC)`, lorsqu’elle est disponible. Le taux réel est inclus. Si le système ne fournit pas de timestamp, les délais restent inconnus.
- `ANALYSIS` : heure `System.nanoTime()` au traitement de l’événement. `Detected.timeSeconds` est la fin du pas RMS déclencheur ; retirer 5 ms donne le **début de ce pas**, pas une attaque physique localisée à ±5 ms. Le seuil peut être franchi après le début réel du son.
- `FRAME` : tick de `withFrameNanos` observé après composition du nouvel état. L’horloge Compose peut normaliser ce temps. Il ne prouve ni la soumission d’une image, ni sa présentation, ni l’allumage des pixels.
- `run` distingue les recréations d’Activity ; `session` distingue les reprises/changements de vis ; `strike` distingue les frappes dans la session.

Ces écarts de temps décrivent le pipeline de diagnostic. Pour impact physique → retour visuel → Hz réellement affichés, filmer simultanément l’instrument et l’écran avec une cadence connue. Ne pas remplacer cette mesure par les temps de fenêtre de 320 ms ni par les ticks Compose.

Commandes (après installation de l’APK debug et lancement) :

```sh
adb -d logcat -s TomTuneTrace:I '*:S' -v raw -T 1 > trace.log
# Arrêter le journal avec Ctrl-C après la série, puis relever run et session dans START/CLOCK.
python3 tools/summarize_strikes.py trace.log --run IDENTIFIANT --session NUMERO --played 10
# Si une méthode indépendante a fourni la fréquence pertinente avec le même geste :
python3 tools/summarize_strikes.py trace.log --run IDENTIFIANT --session NUMERO \
  --played 10 --reference-hz FREQUENCE
```

Le résumé sépare estimations et rejets. La différence entre nombre de détections et nombre de frappes annoncé ne permet pas seule de classer les frappes manquées, faux impacts ou doubles détections : comparer avec la séquence filmée et les observations de l’opérateur. Sans méthode indépendante, ne pas annoncer une erreur de fréquence ni une absence d’erreur de résonance.

Le calcul du script a été vérifié avec une **fixture de métadonnées synthétiques** : une frappe, 0 cent, écarts de 20/340 ms entre le début de pas RMS et les ticks de frame. Ce contrôle arithmétique n’est pas un essai physique.

Le reviewer indépendant a vérifié l’absence d’écriture PCM et a demandé les précisions ci-dessus sur les horloges et les identifiants d’exécution.
