# Rendu 3D — lot 0

Implémentation initiale, sans essai graphique sur appareil dans cette session.

## Géométrie et intégration

- Android : `FilamentTomView`, Filament + gltfio + utils 1.64.1. Le `SurfaceView` est hébergé dans l’écran Compose ; `update(TomSceneState)` met à jour le masque des halos et `close()` interrompt Choreographer et libère la scène. Le `ModelViewer` libère son moteur lors du détachement de sa surface. Vue native à usage unique : la recréer après détachement.
- iOS : `TomSceneViewController`, `ARView(cameraMode: .nonAR)` de RealityKit. Aucune permission caméra/AR n’est demandée. Le host Compose injecte ce contrôleur et appelle `setState(lugCount:selectedLug:)`.
- Coque cylindrique en noyer, staves fines, peau ivoire, cercles supérieur et inférieur, tiges et têtes de vis métalliques. Ombres directionnelles et éclairage de remplissage. Les matériaux sont des approximations procédurales de faisabilité ; ils ne reproduisent pas encore le bois photographique de la référence.
- Caméra fixe en `(0, 2.8, 1.2)` vers `(0, −0.18, 0)` : vue de dessus inclinée d’environ 22° avec coque visible au premier plan. Logo TomTune sur la peau à midi, entre les deux vis supérieures.
- Repère commun : x écran → x monde ; y écran → z monde. Pour i, angle = −π/2 + 2π(i+0.5)/N. Indices zéro-based ; les libellés accessibles du host utilisent i+1.
- Modèles Android vérifiés pour N=5,6,8,10 ; le lot 0 utilise N=8. RealityKit crée la même géométrie paramétrique pour chaque N≥3. Android refuse explicitement un N sans modèle.
- Android sélectionne la géométrie visible avec `View.pick` (coordonnées y inversées), y compris tiges et halo. iOS utilise les collisions natives et un complément de cible de 44 pt autour des têtes projetées. Les deux renvoient le même indice au host ; la sélection du host reste la source de vérité. Les contrôles Compose textuels donnent un second accès accessible.
- Aucun calcul audio sur le callback Choreographer/RealityKit.

## Commandes et résultats

- `python3 tools/generate_tom.py` : succès, quatre fichiers GLB autonomes créés (environ 166 Ko chacun). Générateur Python standard uniquement, aucune ressource distante/licence tierce.
- `swiftc -parse iosApp/TomTune/TomSceneViewController.swift` : succès (syntaxe seulement, pas un build RealityKit/iOS).
- `LugLayoutTest` : nombre, rayon, espacement, symétrie, indices et logo entre les vis supérieures pour 5,6,8,10 ; sélections invalides refusées. Exécution intégrée par l’agent principal.
- `python3 tools/generate_tom.py --check` : succès pour N=5,6,8,10 ; noms/nombre/angles des vis et halos, indices de triangles, accessors/buffers et octets reproductibles. Ce contrôle structurel ne prouve pas le rendu visuel sur GPU.

## Sources consultées

[ModelViewer Filament 1.64.1](https://github.com/google/filament/blob/v1.64.1/android/filament-utils-android/src/main/java/com/google/android/filament/utils/ModelViewer.kt), [Filament picking](https://github.com/google/filament/blob/main/android/filament-android/src/main/java/com/google/android/filament/View.java), [Manipulator](https://github.com/google/filament/blob/main/android/filament-utils-android/src/main/java/com/google/android/filament/utils/Manipulator.java), [RealityKit entity(at:)](https://developer.apple.com/documentation/realitykit/arview/entity(at:)). Les signatures Android sont aussi à valider au build avec les artefacts épinglés.

## Essais restants sur téléphones

1. Démarrer le même écran avec N=5,6,8,10 (état injecté) et toucher chaque vis dans les deux sens ; comparer l’indice accessible à la vis surlignée.
2. Vérifier logo à midi, cadrage en portrait sur plusieurs ratios d’écran, cercle inférieur et coque visibles, absence d’intersection/occlusion gênante. Comparer à `direction-visuelle.png`.
3. Micro actif, frapper doucement près d’une vis du tom réel (autre peau étouffée), sélectionner d’autres vis pendant la décroissance et relever fluidité, latence visuelle, Hz et erreurs de sélection.
4. Quitter/revenir et passer en arrière-plan/réouvrir : absence de frame callback après fermeture, aucune fuite du moteur ou de la vue native, création d’une nouvelle vue après détachement.

Aucun résultat de fluidité, latence ou sélection physique n’est revendiqué avant ces essais.
