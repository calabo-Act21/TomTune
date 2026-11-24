# Essai physique Android du lot 1 — 24 novembre 2026

Appareil connecté : Pixel 9 Pro XL, Android 17. APK debug du commit `2d86954`, empreinte SHA-256 `86e80b2608f0031b64b99f1b2e23eb00f6c51f0651ecdcd59a45ae8d6fa90ef7` vérifiée avant installation. Aucun rebuild : réutilisation de l’APK déjà vérifié.

## Installation et lancement

Commandes exécutées avec ADB sur le téléphone connecté explicitement sélectionné par `-s` :

| Commande | Résultat |
|---|---|
| `adb devices -l` | Pixel 9 Pro XL autorisé, état `device`. |
| `adb shell getprop ro.build.version.release` | `17`. |
| `adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk` | `Success`. Données existantes conservées. |
| `adb shell am start -W -n com.tomtune.app/.MainActivity` | `Status: ok`, lancement à froid, activité `MainActivity`, `TotalTime: 460 ms`. Ce temps ADB ne mesure pas le délai jusqu’à l’affichage physique. |
| `adb shell uiautomator dump /sdcard/tomtune-lot1-ui.xml`, lecture de la hiérarchie | Textes « TomTune », « Configure ton kit », description du stockage local, bouton « Définir mon kit ». |

## Validation fonctionnelle rapportée par le propriétaire

Après installation et lancement sur son téléphone, le propriétaire confirme : « c’et bon j’ai pu valider les features ».

La validation fonctionnelle globale des fonctionnalités du lot 1 sur le Pixel 9 Pro XL est donc rapportée par le propriétaire, sans anomalie signalée dans ce retour. Le premier écran avait également été confirmé par lecture de la hiérarchie Android.

Le retour ne détaille pas séparément les scénarios A1 à A9, le mode avion ou TalkBack. Aucun résultat individuel supplémentaire n’est déduit de cette confirmation globale ; la matrice du compte rendu conserve le détail des vérifications automatisées et sur émulateur.

Aucun essai micro, aucune injection PCM et aucun test acoustique effectué pendant cette session. Aucun changement de code ni nouveau build après la validation ; l’APK testé est celui identifié ci-dessus.
