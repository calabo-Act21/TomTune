# TomTune

Prototype du lot 0 en Kotlin Multiplatform et Compose Multiplatform pour Android et iOS : tom 3D tactile (Filament / RealityKit) et analyse locale d’une frappe près d’une vis.

- [Spécification v0.3](SPEC.md) et [périmètre lot 0](docs/lot0/LOT-0.md)
- [Résultats, commandes et protocole sur téléphone](docs/lot0/RESULTATS.md)
- [Essai physique Android](docs/lot0/ESSAI-ANDROID.md) et [métrologie](docs/lot0/METROLOGIE.md)
- [Paramètres audio et limites](docs/lot0/AUDIO.md), [rendu](docs/lot0/RENDU.md), [contrats](docs/lot0/CONTRATS.md)

JDK 17 requis ; SDK Android API 36 pour Android, Xcode 16.4 sur Mac Apple Silicon pour iOS.

```sh
./gradlew -Pmobile=false :shared:jvmTest
python3 tools/generate_tom.py --check
./gradlew :composeApp:assembleDebug :shared:testDebugUnitTest
xcodebuild -project iosApp/TomTune.xcodeproj -scheme TomTune \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
  -configuration Debug CODE_SIGNING_ALLOWED=NO build
```

Les tests sur signaux et les builds ne remplacent pas les essais micro et 3D simultanés sur téléphone, près d’une vis d’un vrai tom. Aucune référence n’est enregistrée par cet écran.
