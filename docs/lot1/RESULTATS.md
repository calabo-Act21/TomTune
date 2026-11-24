# Livraison du lot 1 Android — 24 novembre 2026

## Base et branche

- Dépôt : `calabo-Act21/TomTune`.
- Départ : `main`, `1d5b263f4ed36beb12a9ff346f4cfac154205fc0` (`prepare lot1`), arbre propre.
- La PR #1 du lot 0 est déjà intégrée par `ce58814`. Son dernier commit `b519e98` est ancêtre du départ. Code et passation sont sur la même base ; aucune intégration de branches n’a été nécessaire.
- Vérifiés avant implémentation : `gradlew`, `composeApp`, `shared`, `SPEC.md` v0.6, filaire HTML, parcours annoté, brief, huit images de kits et catalogue des vues individuelles.
- Branche de travail : `codex/lot1-kits-android`. Livraison commitée localement, sans push, PR ni déclenchement de CI.

## Changements

- Domaine Kotlin partagé : kits/fûts à identifiants stables, révision, état actif, contrat de dépôt, brouillon distinct, règles de diamètre et de vis, tri des positions visuelles par diamètre puis ordre de création.
- Android : premier lancement, configuration 0–3 racks / 0–2 floors, aperçu, nom de 1 à 60 caractères après trim, doublons sans distinction de casse, sauvegarde, changement et édition de kits, navigation vers les fiches. Bouton et action clavier valident le nom. La ViewModel conserve le brouillon lors d’une recréation de l’activité.
- Persistance Android : `files/kits-v1.json`, schéma 1 ; sérialisation de la liste et du kit actif dans un document unique. Écriture sur `Dispatchers.IO`, synchronisation du fichier temporaire puis renommage atomique dans le même répertoire. Une erreur de commit est remontée ; le brouillon et l’état enregistré restent disponibles. Un fichier illisible ou de version inconnue affiche une erreur avec reprise, sans être remplacé par un kit vide. Les pressions rapides ne déclenchent qu’une écriture.
- Visuels : Gradle embarque les originaux depuis `docs/visuels`, sans copies de travail dans le dépôt. Image entière avec `ContentScale.Fit`, zones transformées par le même facteur et le même décalage, association aux IDs triés, libellés accessibles incluant diamètres et vis. Schéma interactif pour zéro floor.
- Fiches : correspondance exacte famille/diamètre/vis, caisse claire 14″ illustrée par la variante 8 vis explicitement illustrative ; données seules hors catalogue. Aucune flèche.
- 43 textes ajoutés dans chacun des catalogues FR/EN/ES/DE. Les deux entrées futures du menu sont désactivées et indiquées « À venir ».
- Moteur audio, tests antérieurs et cibles/entrée iOS conservés. Le code Android de faisabilité est déplacé sans modification fonctionnelle dans `Lot0Activity.kt`. Il n’est pas proposé dans le parcours du lot 1. La déclaration micro préexistante reste au manifeste ; la nouvelle activité n’instancie pas de capture et ne demande aucune autorisation.

## Commandes et résultats

Outillage local préexistant du lot 0, hors dépôt :

```sh
export JAVA_HOME=/private/tmp/tomtune-tools/jdk-17.0.21.1+2/Contents/Home
export GRADLE_USER_HOME=/private/tmp/tomtune-gradle
export ANDROID_HOME=/private/tmp/tomtune-tools/android-sdk
```

| Commande / contrôle | Résultat |
|---|---|
| `git status --short`, `git branch --show-current`, `git log`, `git remote -v`, `git merge-base --is-ancestor b519e98 HEAD` | Base propre, dépôt attendu et ascendance du lot 0 confirmés. |
| `git switch -c codex/lot1-kits-android` | Branche créée. L’écriture des références Git nécessite la sortie du bac à sable configuré en lecture seule pour `.git`. |
| `./gradlew -Pmobile=false :shared:jvmTest --tests 'com.tomtune.kit.KitWorkflowTest' --console=plain` | **5 tests ciblés réussis** : ajout/retrait des fûts et vis manuelles ; choix visuel exact et transformation avec marges ; écriture échouée et double pression ; noms, annulation, édition, sélection et rechargement ; erreur de chargement. Dépôt en mémoire injecté, pas stockage physique. |
| `./gradlew :composeApp:assembleDebug --console=plain` | **BUILD SUCCESSFUL** après les derniers changements Android. Aucun test audio exécuté. |
| `python3 tools/check_lot1_assets.py --apk composeApp/build/outputs/apk/debug/composeApp-debug.apk` | **OK** : 8 compositions, 20 variantes exactes, zones normalisées distinctes, 43 textes dans les 4 catalogues ; assets de l’APK identiques aux sources. |
| `git diff --check` | Réussi. |
| ADB explicite `-s emulator-5554`, `install -r`, `am start -W`, `uiautomator dump`, helper `python3 /private/tmp/tomtune-lot1-smoke.py` puis reprises ciblées `…-smoke-continue.py`, `…-final-smoke.py`, `…-last-check.py` | Voir la matrice ci-dessous et les preuves. Aucun téléphone physique utilisé. |

Le premier démarrage de Gradle et celui d’ADB dans le bac à sable ont échoué avec `Operation not permitted` sur leurs sockets locaux. Les commandes autorisées hors bac à sable ont réussi. Les avertissements Kotlin `expect`/`actual` du lot 0 restent présents. Les premières assertions du helper UIAutomator ont été adaptées à l’état coché porté par le parent du libellé Compose et au préfixe « ← » du bouton Annuler ; ces échecs de helper ne sont pas des exceptions de l’app.

Ni Xcode ni workflow CI ni suite audio n’ont été lancés. Aucun signal PCM injecté, aucune capture micro et aucun essai acoustique. Les essais d’interface sur émulateur sont une **simulation Android**, distincte de la validation physique à faire sur téléphone.

## APK et installation

Chemin exact :

```text
/Users/camlaborde/Workspace/TomTune/TomTune/composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

Sur le téléphone connecté avec le débogage USB autorisé :

```sh
/private/tmp/tomtune-tools/android-sdk/platform-tools/adb install -r "/Users/camlaborde/Workspace/TomTune/TomTune/composeApp/build/outputs/apk/debug/composeApp-debug.apk"
```

Le chemin ADB ci-dessus est celui de cette machine ; un SDK installé ailleurs peut utiliser sa propre commande `adb`. S’il y a plusieurs appareils, utiliser `adb -s SERIAL install -r …`. L’installation `-r` conserve les données existantes. Pour A1, utiliser une installation sans données ; pour A7, conserver les données et forcer seulement l’arrêt. Les kits du prototype HTML ne sont pas importés.

## Matrice A1 à A9

Émulateur Android 15 / API 35 ARM64, locale FR, 1080 × 2400. **Aucune ligne ne constitue une validation sur le téléphone du propriétaire.**

| ID | Vérifié ici | À confirmer sur le téléphone |
|---|---|---|
| A1 | Premier écran « Définir mon kit » ; configuration initiale 1 rack 12″ / 1 floor 16″ sur émulateur. | Installation vierge et lisibilité. |
| A2 | 3 racks + 2 floors, image 3+2 et conservation du brouillon au retour sur émulateur. Règles de diamètre, suggestion et vis manuelles testées dans le domaine ; sélecteur de vis utilisé pour A9. | Changer aussi un diamètre dans l’UI, vérifier clavier et cadrage sur l’écran réel. |
| A3 | Nom vide refusé, nom enregistré et kit sélectionné sur émulateur ; validation clavier Entrée réussie sur le dernier APK. Trim et limite 60 testés dans le domaine. Échec réel d’écriture injecté via un répertoire temporaire à la place du fichier `.new` : erreur lisible, disque inchangé, brouillon conservé, reprise réussie. Double pression couverte par le test ciblé. | Valider également avec le clavier du téléphone ; essayer espaces et 60/61 caractères. |
| A4 | Les 7 zones du kit 3+2 ouvrent le bon type, diamètre et nombre de vis ; retour au même kit sur émulateur. Rectangles distincts et transformation avec marges testés. | Toucher près des bords, précision des zones, TalkBack, redimensionnement et taille d’affichage. |
| A5 | Création 0 rack / 1 floor, doublon `studio` refusé après `Studio`, puis deux kits distincts après renommage sur émulateur. | Refaire la création et comparer l’image 0+1. |
| A6 | Annulation sans changement de composition ; enregistrement de l’édition du seul kit actif ; sélection du premier kit ; IDs, révisions et ordre vérifiés par tests et lecture du JSON Android. | Refaire avec les paramètres choisis et vérifier les deux kits. |
| A7 | Deux kits et dernier kit sélectionné retrouvés après force-stop et relance en mode avion sur émulateur ; mode avion initial restauré. Lecture/écriture du vrai JSON app-privé. | Mode avion, arrêt forcé, relance sur le téléphone. |
| A8 | Kit 0 rack / 0 floor : schéma des seuls caisse claire et grosse caisse, deux fiches accessibles sur émulateur. | Vérifier aussi zéro floor avec des racks et TalkBack. |
| A9 | Rack 12″ à 6 puis 8 vis : image exacte et données correspondantes sur émulateur ; 7 vis : fiche de données seule. Floor et caisse claire ouverts en A4 ; catalogue et exception illustrative de la caisse claire couverts par les contrôles ciblés. | Comparaison visuelle 6/8 vis, famille floor, caisse claire et combinaison hors catalogue sur l’écran réel. |

Preuves de simulation : [kit 3+2](preuves/emulateur-kit-3-2.png), [schéma sans floor](preuves/emulateur-kit-sans-floor.png), [rack 6 vis](preuves/emulateur-rack-12-6.png), [rack 8 vis](preuves/emulateur-rack-12-8.png), [hors catalogue](preuves/emulateur-rack-hors-catalogue.png). Les captures sont inspectées visuellement. Les journaux de build, tests, parcours et le JSON enregistré sont dans `preuves/`.

## Écarts et limites

- Écarts intentionnels au HTML historique, conformes au brief : départ 12″ + 16″, plages de nombre limitées, tabouret hors champ, visuels fournis, fiche de consultation sans mesure ni accordage, entrées futures désactivées, schéma sans floor.
- Le catalogue fourni référence 19 fichiers `.jpg` alors que les fichiers correspondants sont des `.png`. Le chargement Android résout ces références vers les PNG fournis ; les documents sources restent intacts. Le contrôle des assets vérifie cette résolution.
- Présentation Material 3 avec la finition bordeaux des images ; le HTML fixe le parcours et la hiérarchie, pas une reproduction pixel par pixel de son habillage historique.
- Persistance iOS différée selon le lot. Entrée et cibles iOS conservées, sans build Xcode.
- La persistance sur le téléphone du propriétaire, le toucher réel et TalkBack restent à confirmer sur le téléphone. Les références, mesures, accordage et contrôle ne sont pas implémentés au lot 1.
