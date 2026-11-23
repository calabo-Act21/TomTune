# Livraison lot 0 — 23 novembre 2026

## Point de départ et périmètre

Dépôt `calabo-Act21/TomTune`, branche initiale `main`, commit `575d6f21f90dde0fffb8f095e1ff98284db44e10`, arbre propre. Branche de travail `codex/lot0-faisabilite`.

`AGENTS.md`, `SPEC.md` **v0.3**, `docs/lot0/LOT-0.md`, filaire HTML et image de direction lus. Les cas **Résonance secondaire dominante** et **Modes ambigus** sont bien présents. WAV synthétique et CSV huit impacts présents. Les documents de spécification n’ont pas été modifiés.

Un écran Compose partagé, un tom avec huit vis, Filament Android et RealityKit iOS ; sélection native et boutons textuels accessibles ; logo à midi, coque et cercle inférieur. Activation/arrêt du micro, retour de frappe indépendant des Hz provisoires, rejet avec invitation à refrapper. Aucun enregistrement de référence, de kit ou de PCM micro. La cible locale de session 185 Hz est affichée séparément et n’est jamais transmise à l’estimateur. Pas de validation entre frappes.

Les contrats ont été écrits avant les délégations. Audio et rendu ont travaillé en parallèle sur des fichiers distincts. Le principal intègre builds, écran, permissions, cycle de vie, textes et projet Xcode. Voir [CONTRATS.md](CONTRATS.md), [AUDIO.md](AUDIO.md), [RENDU.md](RENDU.md) et [VALIDATION.md](VALIDATION.md).

## Preuves distinctes

| Vérification | Résultat dans cette session |
|---|---|
| Build Android debug | **Réussi** avec SDK temporaire API 36 / build-tools 35.0.0, JDK 17, Gradle 8.14.3. APK `composeApp/build/outputs/apk/debug/composeApp-debug.apk`. Contrôle ZIP : modèles 5/6/8/10, ressources FR/EN/ES/DE et bibliothèques Filament présents. |
| Tests JVM sur signaux et repère | **23 réussis** : 17 audio (dont WAV), 2 repère, 4 cycle de vie/session. Les 22 tests communs réussissent aussi sur la JVM du variant Android. Voir `preuves/integration-tests.log`. |
| WAV synthétique fourni | **8 détections, 8 estimations, 0 rejet, 0 frappe manquée, 0 doublon** ; erreur absolue maximale ≈ **0,014 cent**, tolérance ±12 cents. |
| Délais WAV | Détection à impact +5 ms, résultat +320 ms en **temps du signal injecté**. Ne mesure ni latence matérielle, ni affichage UI, ni charge du GPU. |
| Cas déterministes | Secondaire dominant inharmonique et proche octave, octave exacte, ambiguïté persistante rejetée, fréquence 317 Hz indépendante de la cible 185 Hz, silence, bruit, saturation, décroissance, 44,1 kHz et repliement spectral. |
| Modèles 3D | 4 GLB reproductibles ; buffers, indices, angles et identités contrôlés. Tests du repère pour N=5,6,8,10 réussis. Scène observée dans l’émulateur Android 35 ARM64 / SwiftShader : coque, cercle bas, logo et halo visibles. Voir `preuves/android-emulateur.png`. |
| Build iOS local | **Bloqué** : seul CommandLineTools est installé ; `xcodebuild` exige Xcode complet. `plutil` valide le projet/plist ; `swiftc -frontend -parse` valide la syntaxe Swift, sans vérifier les types UIKit/RealityKit ou le pont Kotlin. |
| CI iOS | **Build, lancement et tests Kotlin natifs réussis** sur Mac ARM64 / Xcode 16.4 / simulateur iOS 18.5 : [run vérifié](https://github.com/calabo-Act21/TomTune/actions/runs/45242956714). Capture `preuves/ios-smoke/smoke.png`, journal `preuves/ios-tests-ci.log`. Aucun micro d’iPhone physique testé. |
| Simulation Android | **Réussie** sur Pixel 7 virtuel Android 35 ARM64 / SwiftShader : sélection native des huit vis, refus puis accord micro, capture AudioRecord et scène simultanées, libération après arrière-plan et fermeture, reprise explicite et picking après réouverture. Ressources/format numérique FR/EN/ES/DE vérifiés. Voir `preuves/android-smoke.log` et `preuves/android-langues.log`. Aucun signal de vrai tom injecté par le micro virtuel. |
| Android physique | **Pixel 9 Pro XL / Android 17**, Filament + AudioRecord simultanés, huit sélections natives, permission refusée/accordée, arrière-plan, fermeture/réouverture et libération vérifiés. Sur caisse claire 14″ timbre débrayé : série exploratoire non comptée (28 détections, 20 estimations, 8 rejets), puis **10 frappes annoncées près d’une même vis → 10 estimations 270,79–271,11 Hz**, plus une détection tardive rejetée `Noise`. [Compte rendu physique](ESSAI-ANDROID.md). |
| Vrai tom et iPhone physique | **Restent à tester**. Les essais physiques disponibles ont utilisé une caisse claire ; pas de référence de fréquence indépendante, ni vidéo impact/écran, ni mesure FPS. Les appels et changements de route audio physiques restent à tester. |

Les seuils sont des paramètres de faisabilité à calibrer sur de vrais toms, pas des garanties produit. Les essais instrumentés ci-dessous restent nécessaires pour conclure sur Kotlin/Compose pour TomTune.

## Commandes exécutées

La machine ne possédait initialement ni JDK fonctionnel, ni Gradle, SDK Android, Xcode complet ou `gh`. Le JDK Temurin 17.0.21.1 et Gradle 8.14.3 ont été téléchargés dans `/private/tmp/tomtune-tools`, le cache Gradle dans `/private/tmp/tomtune-gradle`. Distribution Gradle vérifiée par SHA-256 officiel, empreinte épinglée dans le wrapper. SDK Android temporaire, émulateur et image Google APIs Android 35 ARM64 installés au même endroit via sdkmanager. GitHub CLI temporaire installé pour créer la PR après le refus 403 du connecteur. Les chemins temporaires ne sont pas des prérequis du dépôt.

Pour les commandes Gradle locales, variables utilisées :

```sh
export JAVA_HOME=/private/tmp/tomtune-tools/jdk-17.0.21.1+2/Contents/Home
export GRADLE_USER_HOME=/private/tmp/tomtune-gradle
export ANDROID_HOME=/private/tmp/tomtune-tools/android-sdk
```

| Commande / contrôle | Résultat |
|---|---|
| `git status --short`, `git branch --show-current`, `git rev-parse HEAD`, `git remote -v` | Base et propreté confirmées ; remote attendu. |
| `git switch -c codex/lot0-faisabilite` | Réussi après autorisation d’écriture Git. |
| `java -version`, `xcodebuild -version`, `xcrun simctl list devices available`, `xcrun xctrace list devices`, inventaire SDK/Applications | Java initial absent ; Xcode et outils iOS absents ; pas de SDK Android initial. |
| Téléchargements Gradle/JDK et SDK commandline-tools | Réussis après accès réseau autorisé ; échec DNS dans le sandbox initial. |
| `sdkmanager --sdk_root=… 'platform-tools' 'platforms;android-36' 'build-tools;35.0.0'` | Réussi après acceptation de licence SDK. |
| `gradle -Pmobile=false :shared:jvmTest wrapper --gradle-version 8.14.3 --distribution-type bin --console=plain` | Réussi ; wrapper standard fourni. |
| `./gradlew -Pmobile=false :shared:jvmTest --console=plain` | Réussi, 18 tests après compléments FIR. Voir `preuves/jvm-tests.log`. |
| `./gradlew :composeApp:assembleDebug :shared:testDebugUnitTest --console=plain` | Premier essai échoué : Filament 1.64.0 non publié. Corrigé en 1.64.1 ; build réussi et 17 tests communs Android réussis. Voir `preuves/android-build.log`. |
| `./gradlew :shared:jvmTest :composeApp:assembleDebug :shared:testDebugUnitTest --console=plain` | Réussi : APK compilé, 23 tests JVM et 22 tests unitaires Android, dont 4 tests de session sur chaque cible. Voir `preuves/integration-tests.log`. |
| `unzip -l composeApp/build/outputs/apk/debug/composeApp-debug.apk` | Assets et langues effectivement inclus. |
| `python3 tools/generate_tom.py --check` | Réussi N=5,6,8,10. |
| `swiftc -frontend -parse iosApp/TomTune/*.swift` | Réussi, syntaxe seulement. |
| `plutil -lint iosApp/TomTune.xcodeproj/project.pbxproj iosApp/TomTune/Info.plist` | Réussi. |
| `xcodebuild -project iosApp/TomTune.xcodeproj -scheme TomTune -sdk iphonesimulator -configuration Debug CODE_SIGNING_ALLOWED=NO build` | Bloqué par Xcode absent, voir `preuves/ios-build.log`. |
| `ANDROID_USER_HOME=… adb devices -l` | Serveur démarré, liste vide. |
| `sdkmanager 'emulator' 'system-images;android-35;google_apis;arm64-v8a'`, `avdmanager create avd --name tomtune_lot0 …` | Réussis après acceptation de la licence de l’image ; émulateur headless SwiftShader lancé. |
| `python3 tools/android_smoke.py --adb …/platform-tools/adb --serial emulator-5554` | Réussi sur l’appareil virtuel documenté ; aucun essai physique. |
| `adb shell cmd locale set-app-locales com.tomtune.app --locales fr` (puis es/de/en) et inspection XML UI | Quatre langues et séparateurs numériques vérifiés. |
| `git push -u origin codex/lot0-faisabilite`, `gh pr create --draft …` | Branche publiée ; [PR brouillon #1 et diff](https://github.com/calabo-Act21/TomTune/pull/1/files). |
| Première CI Linux | Détecte différence de derniers bits libm dans les GLB et ancien défaut setup-android demandant le package `tools` retiré. Normalisation à 8 décimales et demande explicite `platform-tools` corrigent ; jobs signaux et Android suivants réussis. |
| Première CI Xcode | Code Kotlin/framework ARM64 compilés ; sync des ressources initialement échouée avec ARCHS générique x86_64. Projet aligné ARM64 puis noms d’enum Swift corrigés ; build, lancement et tests natifs suivants réussis. |
| `./gradlew :shared:iosSimulatorArm64Test --console=plain` sur CI Xcode 16.4 | Réussi ; lancement réel de l’app sur simulateur et capture RealityKit vérifiés. |
| Installation ADB, picking, permissions et cycle de vie sur Pixel 9 Pro XL / Android 17 | Vérifiés sur téléphone ; commandes et valeurs dans [ESSAI-ANDROID.md](ESSAI-ANDROID.md) et les journaux `pixel-*`. |
| `python3 tools/summarize_strikes.py … --session 38 --played 10` | 10 estimations, 1 rejet tardif ; temps de diagnostic étiquetés séparément de la latence impact/écran. |
| `git diff --check` | Aucun défaut d’espacement. |

## Reproduire les builds

Prérequis usuels : JDK 17, accès Maven/Gradle, SDK Android API 36 + build-tools 35.0.0. `ANDROID_HOME` ou `local.properties` pointe vers le SDK ; ne pas committer ce fichier.

```sh
./gradlew -Pmobile=false :shared:jvmTest
python3 tools/generate_tom.py --check
./gradlew :composeApp:assembleDebug :shared:testDebugUnitTest
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

Mac Apple Silicon avec Xcode 16.4 et composants iOS installés (la toolchain Kotlin 2.2.20 est épinglée) :

```sh
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
xcodebuild -project iosApp/TomTune.xcodeproj -scheme TomTune \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -configuration Debug CODE_SIGNING_ALLOWED=NO build
./gradlew :shared:iosSimulatorArm64Test
```

Pour un iPhone : ouvrir `iosApp/TomTune.xcodeproj`, sélectionner l’équipe de signature et l’appareil, lancer le scheme TomTune. Le script Xcode construit le framework `Shared` avec `embedAndSignAppleFrameworkForXcode`. Les cibles iOS sont ARM64 (téléphones et simulateur Apple Silicon). Un simulateur ne valide pas le micro d’un iPhone ni le rendu réel d’un téléphone. La source de test `WavPcmSource(bytes, scope)` peut être substituée au `PcmSource` ; son PCM traverse le même analyseur, sans playback.

## Protocole restant sur téléphones physiques

Effectuer sur Android et iPhone, micro et 3D actifs simultanément, **près d’une vis identifiée d’un vrai tom, peau opposée étouffée**. Consigner : modèle et OS du téléphone, diamètre/type du tom et peaux, vis/indice, placement/distance/orientation du micro, pièce/bruit, méthode de comparaison et fréquence de référence obtenue avec le même geste.

1. Premier lancement : autoriser/refuser le micro ; retour explicite ; autoriser ensuite dans les réglages et reprendre. Une seule capture active.
2. Toucher toutes les vis puis utiliser les boutons accessibles ; vérifier indice, halo, logo, occultations, profondeur et cercle inférieur. Refaire avec N=5,6,8,10 via état injecté. Vérifier petites tailles d’écran, grands textes et langues FR/EN/ES/DE.
3. Filmer à cadence connue l’impact physique et l’écran, avec repère audio ; relever impact → retour visuel, puis impact → Hz (médiane, maximum, dispersion). Les temps du WAV ne remplacent pas ces mesures.
4. Effectuer au moins 20 frappes isolées similaires, puis des amplitudes et distances variées. Pour chaque frappe : détectée/manquée, doublon, Hz ou rejet, fréquence de comparaison, erreur en cents `1200*log2(f/reference)`, octave ou résonance erronée. Séparer les rejets des erreurs acceptées. Respecter ≥400 ms et retour au calme pour ce prototype.
5. Sélectionner des vis pendant la capture, surveiller fluidité GPU et charge audio, mesurer à nouveau les délais. Un résultat est provisoire et n’enregistre rien.
6. Appel/interruption, changement de route (casque/Bluetooth), arrière-plan/retour, écran fermé/réouvert : état compréhensible, reprise explicite, absence de valeurs tardives. Vérifier extinction du témoin micro, absence de capture doublée, de fuite et de callback graphique après sortie. Répéter dix fois.
7. Refaire sur un tom dont un secondaire domine et un cas difficile. Comparer la résonance choisie à une méthode indépendante ; consigner aussi les modes impossibles à départager.

Feuille de relevé (laisser vide tant que non mesuré) :

| Appareil/OS | Tom/vis/placement/référence | Frappes | Manquées | Doublons | Rejets | Mauvais modes/octaves | Erreur cents | Impact→retour | Impact→Hz | Fluidité/lifecycle |
|---|---|---:|---:|---:|---:|---:|---|---|---|---|
| À mesurer Android | | | | | | | | | | |
| À mesurer iPhone | | | | | | | | | | |

**État de faisabilité :** aucun blocage de compilation KMP/Compose observé sur Android ou iOS ; rendu iOS observé sur simulateur, rendu/capture simultanés et mesures par frappe observés sur Android physique. La sélection de la bonne résonance sur un vrai tom, son erreur face à une référence indépendante, les latences physiques et les essais sur iPhone restent nécessaires avant de conclure la faisabilité acoustique et matérielle complète. La cohérence des dix mesures de caisse claire ne valide pas à elle seule le choix du mode.
