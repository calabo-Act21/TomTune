# Revue indépendante du lot 0

Sous-agent `validation` configuré dans `.codex/agents/validation.toml`, revue en lecture seule du diff intégré depuis `575d6f2`, des sources et des preuves. Le principal conserve la responsabilité d’intégration et de livraison.

## Constats et corrections

| Niveau | Constat | Résolution du principal |
|---|---|---|
| P1 | Compose iOS peut planter au lancement sans `CADisableMinimumFrameDurationOnPhone` | Clé `true` ajoutée au plist ; smoke de lancement simulateur ajouté à la CI pour dépasser le simple build. |
| P2 | Changer de vis pouvait afficher une frappe de l’ancienne vis | Chaque changement de vis invalide la mesure et la session précédente. Si le micro était actif, une nouvelle capture est ouverte ; l’analyseur attend un pas calme avant armement pour ignorer une décroissance déjà en cours. Test démarrage/reset pendant décroissance puis vraie frappe ajouté. |
| P2 | Permission iOS différée non invalidée si arrière-plan avant ouverture moteur | Le host observe l’arrière-plan indépendamment d’AVAudioEngine, invalide la génération, ferme et publie `Interrupted` ; callback de permission ancien ignoré. |
| P2 | Après surcharge, `listening=false` pouvait retarder la fermeture jusqu’à un effet Compose | `interrupt()` ferme inconditionnellement la capture, même après une faute, et conserve sa cause. Test surcharge puis arrière-plan vérifie la libération sans attendre l’UI. |
| Observation | Android arrière-plan effaçait la cause | `onStop` passe par `interrupt()` et demande une reprise explicite. |

Les correctifs ont été relus par l’agent, y compris la fermeture inconditionnelle et son test. Sa seconde revue ne trouve aucun nouveau défaut concret P1/P2. Le smoke a été renforcé selon sa remarque : présélection de la vis 2 avant la boucle native et sélection native de la vis 1 après réouverture ; le scénario renforcé passe.

## Portée des preuves

Build Android réel et tests de signaux réussis ; tests de fermeture/reprise avec sources injectées. Le WAV est synthétique ; les temps du signal ne sont pas les latences d’affichage ou matérielles. Les GLB ont des contrôles structurels et le rendu a été observé sous SwiftShader : le reviewer confirme coque, cercle inférieur, logo, halo et contrôles micro visibles simultanément. Cela ne vaut pas une validation sur GPU de téléphone. La CI Xcode a ensuite construit le pont Kotlin/Swift, lancé l’app, produit une capture RealityKit inspectée et exécuté les tests Kotlin natifs.

La revue exige encore des essais natifs de permissions, interruptions/routes, fermeture et rendu/picking, puis des essais simultanés micro/3D sur Android et iPhone près d’une vis d’un vrai tom avec l’autre peau étouffée. L’instrumentation a été revue : aucune écriture PCM, identifiants run/session distincts, diagnostic explicitement limité au pas RMS déclencheur et au tick de l’horloge Compose. Une vidéo est requise pour la latence impact/écran. Aucune justesse de résonance n’est déduite des seuls signaux synthétiques ou de la cohérence des mesures. Procédure et feuille de relevé dans [RESULTATS.md](RESULTATS.md).
