# Livraison Android 2–3 — 24 novembre 2026

## Périmètre et base

Branche locale `codex/lot2-3-android`, issue de `origin/main` à `4868858` après `git fetch origin main`, puis vérification par `git fetch origin`. Le pilote 1.1A fusionné est conservé. Un seul agent. Aucun push, aucune PR ni publication. Le tableau des lots de SPEC.md acte une livraison Android commune pour mesure et guidage.

Lus : AGENTS.md, SPEC.md, LOT-2-3.md, PARCOURS.md, HTML du filaire, charte 1.1A, index, catalogue et ancrages. La lecture du HTML fixe les transitions ; son ouverture `file://` dans le navigateur intégré a été refusée par la politique du navigateur. Aucun contournement ni contrôle interactif du HTML revendiqué. L’interface Android a été contrôlée sur émulateur avec captures.

L’inventaire initial avait été effectué sur l’ancienne copie locale de main. Les **26 déclinaisons sont bien présentes sur le main actualisé**. Elles sont intégrées, sans production ni modification d’image. Voir les vérifications ci-dessous et l’index 1.1 mis à jour.

## Comportement livré

- Depuis la fiche d’un rack ou floor tom : référence exacte ou suggestion par diamètre → choix de cible temporaire MIDI 28 à 60 → frappe et ses vis → conseils et nouvelle passe → résonance et ses vis → centre → comparaison, remesure ou correction des deux peaux → fin et retour au même tom. Les fiches affichent l’état, la référence, la dernière mesure et leur écart. Caisse claire et grosse caisse n’ont pas d’action d’accordage guidé.
- Profil v1 figé pour la session : frappe ×1,50, résonance ×1,85. Les Hz stockés ne sont jamais arrondis à la note affichée. La cible de session ne remplace pas la référence au démarrage ni pendant les mesures.
- Même estimateur TomStrikeAnalyzer, sans biais de cible. Bandes distinctes : vis 55–900 Hz, centre 35–550 Hz. Événements consommés dans l’ordre avant la publication de l’état d’affichage. Détection immédiate, valeur provisoire, rejet motivé ou instabilité, puis confirmation. Trois frappes si dispersion ≤8 cents, cinq si ≤18 cents ; au-delà, redémarrage avec la dernière estimation ; médiane des Hz.
- Une confirmation avance vers **une seule prochaine vis non mesurée**, dans l’ordre horaire. Dernière confirmation visible ; toucher d’une vis pour la remesurer, confirmations du tour conservées ; nouveau tour vide. Chaque réarmement invalide la génération précédente, les détections en attente et la série. Les événements sans détection, doublons et retours tardifs ne font pas avancer. Une discontinuité PCM impose une reprise à série vide. Une correction efface le résultat central et exige une nouvelle mesure avant la fin.
- AndroidPcmSource derrière PcmSource pour le lancement normal. Permission rattachée à sa demande exacte, une capture maximum, arrêt au résultat, aux conseils, à l’arrière-plan et à la sortie ; reprise explicite au même stade. Un ViewModel garde la session lors des changements de configuration. Une session temporaire n’est pas restaurée après destruction du processus ; aucun brouillon ne modifie les valeurs enregistrées.
- « Terminer » écrit la mesure centrale stable. L’action explicite qui affiche la cible et l’enregistre comme référence écrit les deux valeurs ensemble. En cas d’échec, résultat et actions restent disponibles. Sérialisation des écritures, double appui dédupliqué et contrôle des IDs/configuration. Le codec lit le schéma 1 historique avec champs optionnels absents. Le même fichier `kits-v1.json`, mutex, synchronisation et renommage atomique sont conservés. Un diamètre changé efface référence et mesure ; un renommage les conserve. Les mesures de vis restent propres à chaque session/tour.
- Fond uni `#E3D1CA` sous toutes les barres système. Nouvelles ressources et notation des notes en FR, EN, ES et DE ; nombres formatés à l’affichage, valeurs et IDs indépendants de la langue.

## Visuels

28 PNG alpha embarqués au total (deux pilotes et 26 déclinaisons). Pour les 26 nouvelles images : dimensions égales à leurs sources, **zéro différence RGB** ; seul l’alpha change. Les anciens pilotes sont préservés. Contrôle binaire d’identité des fichiers dans l’APK, sans recompression des sources par notre préparation d’assets.

Sept cartes de kit ajoutées sur le modèle du pilote, vérifiées par superposition peau/coque et ordre d’occlusion. Chaque carte référence son image exacte, ses dimensions, son empreinte et sa validation technique. Le rendu et les IDs restent une publication indivisible ; protections de chargement/tardivité du 1.1A conservées. Zéro floor et échec de chargement gardent leur schéma adapté.

Guidage photo activé uniquement pour **la frappe rack 12″ / six vis**, après inspection des six têtes sur le PNG alpha exact et publication de son manifest propre. Les autres faces utilisent le schéma avec le vrai nombre de vis (3 à 12). Les autres images alpha servent aux fiches, sans activer des ancrages non vérifiés. Flèche Compose, toucher 48 dp et accessibilité prennent le même point et le même repère proportionné. Les cartes sont techniquement vérifiées ; précision tactile sur téléphone et TalkBack restent physiques.

Preuves : [cartes superposées](preuves/cartes-kits.png), [RGB et alpha](preuves/assets-rgb.log), [index et règles d’activation](../visuels/lot1.1/INDEX.md).

## Commandes et résultats

Environnement local réutilisé :

```sh
export JAVA_HOME=/private/tmp/tomtune-tools/jdk-17.0.21.1+2/Contents/Home
export GRADLE_USER_HOME=/private/tmp/tomtune-gradle
export ANDROID_HOME=/private/tmp/tomtune-tools/android-sdk
```

| Vérification exécutée | Résultat |
|---|---|
| `./gradlew -Pmobile=false :shared:jvmTest --console=plain` | **40 tests réussis**, dont tous les tests antérieurs conservés. [Journal](preuves/jvm-tests.log). |
| `./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug --console=plain` | **2 tests du codec réussis**, lecture réelle d’un ancien snapshot, aller-retour des Hz exacts, valeurs invalides refusées. Build réussi. [Journal](preuves/android-tests-build.log). |
| `./gradlew :composeApp:assembleDebug --console=plain` | Build final réussi après passage de la source de diagnostic sur worker, avec invalidation synchrone des callbacks. [Journal](preuves/build-debug.log). |
| `python3 tools/check_lot11_assets.py --apk composeApp/build/outputs/apk/debug/composeApp-debug.apk` | Contrôles historiques préservés : sources et pilotes identiques dans l’APK. |
| `python3 tools/check_lot23_assets.py --apk composeApp/build/outputs/apk/debug/composeApp-debug.apk` | 28 PNG, huit cartes de kit avec points peau/coque et priorité des recouvrements, manifest des six vis, 127 clés dans chacune des quatre langues, paramètres de format identiques et fichiers embarqués exacts. |
| `python3 tools/verify_lot23_emulator.py --adb "$ANDROID_HOME/platform-tools/adb" --output docs/lot2-3/preuves/emulateur` | Parcours UI complet avec **PCM synthétique injecté**, nouvelle passe, deux peaux, centre, correction des deux peaux, nouvelle mesure, fin sur la même fiche et relance normale. Arrière-plan/reprise et conservation des autres kits/fûts vérifiés. [Journal](preuves/emulateur.log). |
| `python3 tools/verify_lot23_permissions.py --adb "$ANDROID_HOME/platform-tools/adb" --output docs/lot2-3/preuves/permissions` | **Émulateur, chemin micro normal** : refus puis accord, arrêt effectif en arrière-plan (`appops` sans `running`), reprise explicite, annulation puis confirmation d’abandon ; snapshot identique. Aucun essai acoustique. [Journal](preuves/permissions.log). |
| Helpers `/private/tmp/lot23-schema-ui.py` puis `/private/tmp/lot23-schema-finish.py`, ADB ciblé `emulator-5554` | Schéma floor 16″ à huit cibles accessibles, texte 200 %, commandes accessibles en défilement ; taille restaurée à 1,0 et abandon sans écriture. [Journal](preuves/schema.log). |
| `apksigner verify --print-certs` sur l’APK final et une copie en lecture du 1.1A installé sur le Pixel | **Certificats SHA-256 identiques** : `6b8fae72dbd4481071bde7fbf160eb67c811e8de1d7b79a59cd585cf08f15c42`. |
| `adb -s PHONE_SERIAL install -r …`, puis ouverture normale de MainActivity | **Success** sur Pixel 9 Pro XL / Android 17 / API 37 ; snapshot des kits strictement identique avant/après ; démarrage à froid OK. [Relevé](preuves/installation-pixel.log). |
| `git diff --check` | Réussi. |

Les sockets Gradle/ADB et les écritures Git ont nécessité les escalades du bac à sable. Un premier build avait relevé des smart casts Kotlin entre modules ; corrigés avant les builds réussis. Les premières tentatives du helper UI cherchaient les boutons sous le défilement sans défiler ; le helper a été corrigé. Après retour de police à 100 %, un Back a été envoyé avant la fin du changement de configuration ; la continuation attend l’activité puis vérifie l’abandon et le snapshot. Ces tentatives ne sont pas revendiquées comme réussies ; les journaux finaux et la continuation attestent les résultats décrits.

Le test JVM injecté complet utilise une source PcmSource avec décroissances synthétiques à 8 kHz, les mêmes contrôleurs et le même analyseur que le micro : floor 18″ / trois vis hors catalogue, nouvelle passe, deux peaux, centre à **42 Hz**, correction, centre à **120,00072 Hz** pour une cible inchangée à **65,40639 Hz**, puis persistance et retour au même ID. La fréquence peut donc être confirmée loin de la cible. Le parcours Android injecté utilise des frappes espacées d’environ une seconde et affiche explicitement « PCM injecté · microphone désactivé ». Il est accessible uniquement par extra d’intent explicite dans une app débogable ; le lancement normal utilise toujours le micro et n’enregistre pas ce choix.

Les tests couvrent les séries courtes/longues/incohérentes, rejets, doublons, changement de vis pendant une frappe, discontinuité, toutes les erreurs de capture et reprise, retour synchrone de refus, référence exacte hors sélecteur, cible différente de référence (exemple −49 et +151 cents), deux fins, échec/réessai, correction, abandon, configuration incompatible et schémas 3 à 12 vis. Les tests antérieurs de silence, bruit, saturation, modes, octave, décroissance et réarmement restent passants. Cadence injectée qualifiée : environ 1 Hz avec retour au calme ; le délai de réarmement de l’analyseur reste 400 ms et ne constitue pas une promesse de cadence en acoustique réelle.

## Livraison et limites physiques

Un seul APK final : `composeApp/build/outputs/apk/debug/composeApp-debug.apk`. Empreinte dans [apk.sha256](preuves/apk.sha256). Le commit source est celui indiqué dans la réponse de livraison et par `git rev-parse HEAD` sur la branche locale. Le fichier est directement accessible sur la même machine que le Pixel ; aucun hébergement ni publication n’est nécessaire.

```sh
/private/tmp/tomtune-tools/android-sdk/platform-tools/adb install -r "/Users/camlaborde/Workspace/TomTune/TomTune/composeApp/build/outputs/apk/debug/composeApp-debug.apk"
```

Si plusieurs appareils sont présents, insérer `-s SERIAL` avant `install`. L’installation déjà effectuée était explicitement ciblée sur le Pixel ; les scénarios automatisés sur `emulator-5554` n’ont jamais modifié les kits du téléphone.

La [recette unique préparée](RECETTE-ANDROID.md) tient compte de l’instrument annoncé ensuite par le propriétaire : **caisse claire 14″ pour tester la frappe**. Cet essai local est à réaliser, timbre débrayé et peau opposée étouffée, puis abandon de la session temporaire. Il ne valide pas le cycle d’un vrai tom. Le chemin dédié caisse claire, le contrôle du kit, le gestionnaire général des références et iOS restent hors lot.

Aucune justesse, fausse validation, erreur d’octave, latence acoustique ou calibration physique n’est déclarée validée. Le profil et les seuils restent des hypothèses debug. Le cycle complet et la sélection de fréquence au centre avec deux peaux libres restent à valider sur un vrai tom ; les repères tactiles, TalkBack et le confort d’utilisation sur le Pixel restent également physiques.
