# Correction de la mesure près d’une vis — 25 novembre 2026

## Provenance

[Départ](https://github.com/calabo-Act21/TomTune/commit/97dce457e11372f89de5d1a48c874cfbc4ba799f), snapshot complet du lot 2–3 + pilote photo non commité auparavant. L’ancien APK correspond au SHA-256 du journal d’installation du pilote ; son commit exact n’est pas prouvable. Voir [limites de provenance](DEPART-MESURE-VIS.md). Aucun merge dans main.

## Comparaison réelle avec le lot 0

Référence lot 0 : `b519e98c935ad341f008cdc26d33b41a6f724d02`. `git diff b519e98 97dce45 -- shared/src/androidMain/kotlin/com/tomtune/audio shared/src/commonMain/kotlin/com/tomtune/audio` est vide. Le PCM du retour acoustique utilisateur n’a pas été enregistré ; il ne peut pas être rejoué.

| Chaîne | Lot 0 | Départ lot 2–3 | Correction locale |
|---|---|---|---|
| AndroidPcmSource | AudioRecord mono PCM16, 48 kHz demandés / taux réel fourni ; UNPROCESSED si disponible sinon VOICE_RECOGNITION ; blocs de 480 échantillons | Identique ; entrée principale et permissions raccordées au guidage | Identique, PCM en mémoire |
| Détection | RMS 5 ms ; seuil 0,018 ; bruit ×5 ; enveloppe ×2,2 ; réarmement 400 ms et RMS <0,010 | Identique | Seuil local 0,010 ; autres règles identiques ; centre inchangé |
| Analyse | Capture 320 ms ; fenêtres Hann 128 ms à 20/84/148 ms ; filtrage avant décimation 8 kHz ; FFT 4096 ; concentration ≥0,45 | Identique | Identique |
| Bande | 80–700 Hz partout | 55–900 Hz vis, 35–550 Hz centre | Bandes conservées |
| Choix | Modes persistants ; famille harmonique choisie au plus grave, sinon avantage de persistance ≥2,5 | Identique, sans cible passée à l’analyseur | Avantage de persistance ≥2,5 requis aussi entre modes d’octave ; sinon AmbiguousModes. Aucun rabattement d’octave sans pic, aucun choix par la cible |
| Série | Aucune confirmation entre frappes | 3 valeurs/8 cents ou 5/18 cents, médiane ; stop micro puis avance automatique | Même stabilité ; fenêtre bornée à cinq valeurs après confirmation ; brouillon affinable ; contradiction remet la série à 1 ; « Suivant » seul valide et avance |
| Contrôleur | Worker ordonné ; génération capture | Paramètres par contexte, événements ordonnés avant StateFlow, trous PCM signalés Overrun, générations de vis | Ces protections sont conservées ; pas de redémarrage micro à la confirmation locale |
| Trace | Métadonnées dans Lot0Activity | Pas de trace par frappe dans le parcours principal | TomTuneTrace sur build debug : CaptureStart, Detected, Rejected/reason, Estimate/hz/confidence, SeriesReset/reason, Confirmed/hz/count, Next |

La bande élargie peut rendre visible un mode plus aigu auparavant hors bande ; cela ne prouve pas la cause des lectures physiques 350/700 Hz. La règle harmonique forcée peut sous-estimer un mode plus aigu lorsque le mode inférieur est présent. Le seuil local abaissé est une hypothèse contrôlée par injection ; le gain physique et les faux déclenchements restent à observer.

## Vérifications ciblées

Résultats à compléter après exécution des commandes dans `preuves/mesure-vis/`. Les tests physiques restent distincts des tests synthétiques et de l’émulateur.

Première passe JVM : un scénario de doublons utilisait des ID artificiels 10000/10001 avant de revenir à 2 ; le suivi borné par ID monotone les refusait. Le scénario suit maintenant les ID croissants de l’analyseur réel et conserve les mêmes assertions de doublons, rejet et événements tardifs. La première tentative reste dans `tests-jvm-initial.log` ; la suite corrigée est passante.

### Résultats réellement observés, sources distinctes

| Vérification | Résultat |
|---|---|
| Même PCM synthétique, analyseur lot 0 figé en test seulement, départ lot 2–3 et correction | 350 Hz seul et 700 Hz seul conservés. Deux modes persistants 350/700 sans avantage de décroissance : anciens analyseurs ≈350,0013 Hz, correction rejet `AmbiguousModes`. Mode aigu étayé par la décroissance malgré un pic inférieur résolu : anciens ≈350,0011 Hz, correction ≈700,0011 Hz. |
| Dix coups faibles injectés, amplitude normalisée 0,025, intervalles PCM de 2 s | Lot 0 et départ : 0 détection, 10 impacts attendus manqués. Correction locale : 10 détections, 0 rejet, ≈350,0013 Hz ; première stabilité au coup 3. Ce n’est pas une échelle de force physique. |
| Dix coups synthétiques sous le nouveau seuil, amplitude 0,008 | 0 détection, 10 impacts attendus manqués, aucune estimation. Aucun seuil ne permet de récupérer un signal absent ou noyé dans le bruit. |
| Série locale par PcmSource injectée, deux cibles MIDI 28 et 60 | Dix coups faibles : stabilité au 3e ; rejet 350/700 sans ajout à la série ; première frappe 700 relance à 1 et conserve l’ancienne stabilité 350 ; confirmation 700 après 3 frappes exploitables. Même vis et capture jusqu’à « Suivant ». |
| PCM natif 48 kHz injecté | Mode 700 seul conservé ; modes 350/700 ambigus rejetés ; frappe faible 350 détectée. |
| Silence, bruit large bande, saturation avec paramètres locaux | Aucune estimation acceptée ; Noise / Clipped distincts. Anciens tests conservés, y compris la politique centrale historique. |
| Android / émulateur API 36 / PCM injecté en mémoire | Dix coups faibles sur vis 1, stabilité au 3e, aucun avancement au 10e ; coup 11 ambigu rejeté ; coups 12–14 à 700 Hz, provisoire puis stable, ancienne confirmation 350 visible et bouton réellement désactivé pendant la nouvelle série. Un appui valide et ouvre vis 2. |
| Android / deux peaux / PCM injecté | Trois vis par peau sur schéma exact ; six brouillons stables acceptés par six appuis sur « Suivant », conseils de résonance atteints ; aucun parcours central ajouté à cette recette. Abandon sans modification du snapshot du kit de diagnostic. |
| Tests | 46 JVM + 42 unitaires shared Android + 2 unitaires app Android, aucun échec final. Build debug réussi. Les suites antérieures sont conservées ; leurs transitions attendent maintenant l’appui explicite. |
| Métadonnées de série comptée | Fixture de journal synthétique : créneau sans détection distinct d’un rejet Noise ; Hz, remise à zéro et confirmation séparés. Aucune déduction d’impact manqué sans tops opérateur. |
| Signature | Certificat SHA-256 identique à l’ancien APK du pilote : `6b8fae72dbd4481071bde7fbf160eb67c811e8de1d7b79a59cd585cf08f15c42`. |
| Téléphone / instrument physique avec nouvel APK | **Non réalisé : aucun Pixel connecté à ADB pendant cette intervention.** Aucun Hz ni compte acoustique inventé. Retour existant sur ancien APK seulement : lectures autour de 350–700 Hz et refrappes fréquentes, déclaré par le propriétaire. |

Les tests JVM réutilisent le calcul et les contrôleurs de production. La copie test de l’analyseur lot 0 est identique à `b519e98` sauf son package ; elle n’est pas embarquée dans l’APK. Les traces Android portent `input=injected`. La cadence murale de l’émulateur peut dériver avec son ordonnanceur ; les deux secondes indiquées décrivent le PCM synthétique, pas une mesure de cadence acoustique.

Commandes exécutées (environnement JDK 17 / SDK 36 restauré sous `/private/tmp/tomtune-tools`, `GRADLE_USER_HOME` temporaire) :

```sh
./gradlew -Pmobile=false :shared:jvmTest --console=plain
./gradlew :composeApp:assembleDebug :shared:testDebugUnitTest :composeApp:testDebugUnitTest --console=plain
python3 tools/verify_lug_measurement_android.py --adb /private/tmp/tomtune-tools/android-sdk/platform-tools/adb --output docs/lot2-3/preuves/mesure-vis/emulateur
python3 tools/summarize_lug_trace.py docs/lot2-3/preuves/mesure-vis/emulateur/probe-trace.log
python3 tools/check_lot23_assets.py --apk composeApp/build/outputs/apk/debug/composeApp-debug.apk
# apksigner verify --print-certs sur le pilote sauvegardé et l’APK corrigé
git diff --check
```

Journaux et captures : [tests JVM](preuves/mesure-vis/tests-jvm.log), [build/tests Android](preuves/mesure-vis/build-android.log), [recette émulateur](preuves/mesure-vis/emulateur.log), [trace par frappe](preuves/mesure-vis/emulateur/probe-trace.log), [résumé](preuves/mesure-vis/probe-summary.json), [signature](preuves/mesure-vis/signature.log).

La première assertion UI cherchait le bouton sous le défilement ; la suivante lisait l’attribut enabled du texte enfant au lieu du conteneur bouton. Les tentatives restent dans `emulateur-initial.log` et `emulateur-bouton-texte.log`. Le contrôle final lit le conteneur et la lecture/« Suivant » sont désormais fixes sous le contenu défilant. La dernière lecture exploitable reste affichée pendant l’analyse du coup suivant, explicitement étiquetée comme précédente, pour éviter un effacement et un saut de mise en page à chaque détection.

### Limites et prochaine action physique

Le seuil RMS et le critère de persistance restent des hypothèses debug. La répétabilité de signaux synthétiques ne valide ni la résonance locale de la caisse claire, ni la justesse d’un mode de tom, ni les fausses détections en bruit réel. La capture Android, le centre, les cibles et la persistance métier restent dans leur périmètre antérieur. La sélection conservatrice entre modes d’octave est activée seulement pour la mesure près d’une vis.

La [session physique unique](RECETTE-ANDROID.md) commence par dix coups sur la même vis, baguette à 2,5 cm du cercle, environ toutes les deux secondes, micro fixe, puis coups plus francs et éventuel tom pendant la même session. Elle reste à réaliser avec le seul APK final. Aucun accordage guidé de caisse claire n’est ajouté.

### Livraison identifiable

Le build final est effectué depuis un commit propre. `assets/build-info.properties` embarque `sourceCommit` et `worktreeDirty=false`. Le commit de preuves suivant peut documenter l’empreinte sans changer les sources embarquées. Le seul APK à installer est `composeApp/build/outputs/apk/debug/composeApp-debug.apk` ; l’ancien APK du pilote a été sauvegardé uniquement pour comparaison, hors dépôt. L’empreinte de la nouvelle livraison remplace celle de `preuves/apk.sha256` lors de la clôture ; les SHA dans les anciens relevés d’installation restent historiques.
