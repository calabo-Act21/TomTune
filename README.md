# TomTune

Projet Kotlin Multiplatform et Compose Multiplatform. Le lot 1 Android propose la création, la sauvegarde locale, la sélection et l’édition de kits, une vue interactive et les fiches des fûts. Le moteur audio et les cibles iOS du lot 0 sont conservés.

- [Spécification v0.6](SPEC.md) et [brief du lot 1](docs/lot1/LOT-1.md)
- [Livraison Android lot 1, APK et validation A1–A9](docs/lot1/RESULTATS.md)
- [Périmètre du lot 0](docs/lot0/LOT-0.md)
- [Résultats, commandes et protocole sur téléphone](docs/lot0/RESULTATS.md)
- [Essai physique Android](docs/lot0/ESSAI-ANDROID.md) et [métrologie](docs/lot0/METROLOGIE.md)
- [Paramètres audio et limites](docs/lot0/AUDIO.md), [rendu](docs/lot0/RENDU.md), [contrats](docs/lot0/CONTRATS.md)

JDK 17 requis ; SDK Android API 36 pour Android, Xcode 16.4 sur Mac Apple Silicon pour iOS.

Build courant du lot 1 et installation sur un téléphone connecté :

```sh
./gradlew :composeApp:assembleDebug --console=plain
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

L’application Android ouvre le parcours de kits et ne demande pas le micro. L’implémentation de l’écran de faisabilité reste dans `Lot0Activity.kt` et `App.kt`, sans entrée dans le parcours du lot 1. La persistance iOS est différée ; son point d’entrée existant est conservé.

Commandes historiques du lot 0 (ne pas exécuter cette suite pour un simple changement de kit) :

```sh
./gradlew -Pmobile=false :shared:jvmTest
python3 tools/generate_tom.py --check
./gradlew :composeApp:assembleDebug :shared:testDebugUnitTest
xcodebuild -project iosApp/TomTune.xcodeproj -scheme TomTune \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -configuration Debug CODE_SIGNING_ALLOWED=NO build
```

Les tests sur signaux et les builds ne remplacent pas les essais micro et 3D simultanés sur téléphone, près d’une vis d’un vrai tom. Aucune référence n’est enregistrée par cet écran.
