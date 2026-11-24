# Contrats d’intégration lot 0

Définis avant délégation. `shared/.../contracts` appartient à l’agent principal.

- `PcmSource.start(PcmSink)` reçoit des blocs mono normalisés, copiés, ordonnés, avec taux réel et position en frames. Les adaptateurs invoquent le sink hors thread UI. `stop()` invalide synchroniquement la session et libère le périphérique. Permission acquise par le host ; les erreurs utilisent `CaptureIssue`.
- `StrikeAnalyzer.accept` et `finish` produisent `Detected` avant le résultat. Un identifiant par frappe, zéro ou un résultat `Estimate`/`Rejected`. Pas de cible dans la décision de fréquence. Analyse sans stockage de PCM sur disque.
- `TomSceneState` et `TomSelectionSink` relient les scènes natives au même écran Compose. Indices 0…N−1, angle −π/2 + 2π(i+0,5)/N, écran y vers le bas ; logo à midi. Boutons Compose accessibles équivalents.
- Android : `FilamentTomView(context, onSelect: (Int)->Unit)` avec `update(state)` et `close()` dans `com.tomtune.scene`. iOS : Swift `TomSceneViewController` avec `setState(lugCount:selectedLug:)` et closure `onSelect`. Le host injecte une UIViewController dans Compose via factory ; Compose transmet les changements de sélection.
- Audio : classe commune `TomStrikeAnalyzer` dans `com.tomtune.audio`, implémentant `StrikeAnalyzer`. Android `AndroidPcmSource(context)` ; iOS Swift `IOSPcmSource` conforme à `PcmSource` du framework `Shared`, AVAudioEngine ; le host Swift fournit la permission et le pont de scène. Les entrées PascalCase de `CaptureIssue` sont exportées en minuscules (`permissiondenied`, `routechanged`) par Kotlin/Native.

Propriété : audio → `shared/src/**/…/audio/**`, `iosApp/TomTune/IOSPcmSource.swift`, `docs/lot0/AUDIO.md` ; rendu → `shared/src/**/…/scene/**`, `iosApp/TomTune/TomSceneViewController.swift`, `tools/generate_tom.py`, ressources de modèle, `docs/lot0/RENDU.md` ; principal → builds, contrats, écran, hosts, ressources texte, projet Xcode et livraison. Ne pas modifier les fichiers d’un autre périmètre sans coordination.
