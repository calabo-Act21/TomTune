# Audio lot 0 — paramètres de faisabilité

L’analyse est du Kotlin commun. `TomStrikeAnalyzer` consomme les mêmes `PcmBlock` depuis AudioRecord, AVAudioEngine ou `WavPcmSource`. La cible affichée n’entre pas dans son API : aucune attraction vers 185 Hz. Les fréquences ne sont ni une référence enregistrée, ni une validation entre frappes. Aucun PCM micro n’est écrit sur disque.

## Détection et décision

- PCM mono normalisé, taux réel de 8 à 192 kHz, temps en frames. Blocs contigus ; un changement de taux ou une discontinuité réinitialise l’analyse.
- RMS calculée toutes les 5 ms, attaque si RMS > 0,018, > 5 × bruit estimé et > 2,2 × enveloppe précédente. Bruit initial 0,001, lissage 0,97/0,03 sous le seuil d’attaque ; enveloppe avec facteur 0,88 par pas. Ces seuils d’amplitude dépendent du gain et du téléphone.
- `Detected` est retourné immédiatement à la fin du pas d’attaque, sans attendre les Hz. Une frappe conserve 320 ms de PCM maximum et produit un seul résultat. Le retour UI passe ensuite par la file de l’écran.
- Au démarrage et après chaque réinitialisation (dont changement de vis), l’analyseur attend au moins un pas calme de 5 ms avec RMS < 0,010 avant de s’armer : une décroissance déjà en cours ne devient pas une nouvelle attaque. Réarmement après une frappe après 400 ms minimum et RMS < 0,010. La décroissance ne peut donc pas devenir une seconde frappe. Le lot traite une frappe à la fois : les frappes superposées ou très rapprochées ne sont pas séparées et doivent être rejouées.
- Fenêtres Hann de 128 ms commençant à +20, +84 et +148 ms après le début du pas d’attaque. Le transitoire initial est ignoré. Résultat lorsque 320 ms sont disponibles. Une fin de fichier à moins de 280 ms donne `TooShort`.
- Filtre FIR passe-bas 1 600 Hz (sinc fenêtré Hamming, rayon `ceil(taux/8000 × 8)` échantillons), puis rééchantillonnage à 8 kHz. FFT radix-2 4 096 points après remplissage de zéros ; résolution physique des fenêtres ≈ 7,8 Hz. L’interpolation parabolique du logarithme des trois puissances autour du pic améliore la localisation des sinusoïdes isolées ; le remplissage de zéros ne sépare pas des modes physiquement indiscernables.
- Recherche locale 80–700 Hz, six pics maximum par fenêtre, puissance > 3,5 % du maximum. Une bande ±4 bins autour des pics doit contenir ≥ 45 % de la puissance totale. Cette concentration rejette le bruit large bande ; elle n’est pas une preuve suffisante de justesse sur un tom.
- Un candidat doit être retrouvé dans les trois fenêtres à ±35 cents et rester à ≥ 3,5 % du plus fort pic tardif. Des modes réellement proches ou comparables ne sont pas départagés par proximité de cible ou simple amplitude.
- Si plusieurs composantes forment une famille harmonique (rapports entiers à ±20 cents), une composante basse réellement observée soutient la période basse. Aucun sous-harmonique absent du spectre n’est inventé.
- Pour des modes inharmoniques concurrents, on exige un avantage de persistance ≥ 2,5 sur le ratio puissance tardive/précoce. Sinon `AmbiguousModes` invite à refrapper. Cela couvre un mode secondaire initialement plus fort mais plus rapidement amorti ; un secondaire durable sans autre indice reste ambigu.
- Saturation : > 0,5 % d’échantillons à |PCM| ≥ 0,985 rejette la frappe entière. La confiance exposée est une concentration spectrale indicative, sans interprétation probabiliste calibrée.

Ces paramètres sont des hypothèses de faisabilité. Ils doivent être calibrés près des vis sur de vrais toms (diamètres, peau, gain micro, modes inharmoniques, distance et environnement), avant d’étendre la bande ou la validation entre frappes. Le choix du mode local ne peut pas être garanti par les seuls exemples synthétiques.

## Sources et cycle de vie

Android lit des paquets de 480 échantillons PCM16 sur `TomTune-PCM`, AudioRecord à 48 kHz demandé et taux réel transmis. Source `UNPROCESSED` lorsque prise en charge, sinon `VOICE_RECOGNITION`. Le buffer matériel a au moins quatre fois la taille minimale. Permission acquise par le host ; refus, lecture échouée, silence imposé par le système (API 29+) et route modifiée émettent un problème et ferment la capture. `stop()` invalide les callbacks sous verrou puis arrête/libère le recorder. Sur Android 26–28, les interruptions sans erreur de lecture doivent encore être vérifiées physiquement ; le host ferme aussi la capture à la sortie et en arrière-plan.

iOS utilise AVAudioEngine, session `.record`/`.measurement`, durée IO demandée 10 ms. Le tap copie et moyenne les canaux ; la conversion Kotlin et l’analyse ne tournent ni sur UIKit/RealityKit ni dans le tap. Une file série bornée à 32 blocs signale `Overrun` si l’analyse ne suit pas. Notifications d’interruption, route et arrière-plan ferment le moteur et désactivent la session. L’invalidation sous verrou et le nettoyage sur la file moteur empêchent les callbacks tardifs ; le nettoyage ne tient pas le verrou dont le tap a besoin.

`WavPcm.decode` lit strictement RIFF PCM16 mono, y compris des chunks annexes. `WavPcmSource(bytes, scope)` injecte des blocs de 10 ms sur `Dispatchers.Default`, sans playback, avec annulation et invalidation synchrone. Le WAV est une source synthétique, aucune capture réelle.

## Vérifications

Commande exécutée par l’agent audio (JDK/Gradle téléchargés hors dépôt) :

```sh
JAVA_HOME=/private/tmp/tomtune-tools/jdk-17.0.21.1+2/Contents/Home \
GRADLE_USER_HOME=/private/tmp/tomtune-gradle \
/private/tmp/tomtune-tools/gradle-8.14.3/bin/gradle -Pmobile=false \
  :shared:jvmTest wrapper --gradle-version 8.14.3 --distribution-type bin --console=plain
```

Premier passage avant ajout du FIR et des tests 44,1 kHz/anti-repliement : **BUILD SUCCESSFUL**, 13 tests audio et 2 tests géométriques. Le rapport d’intégration du principal donne le résultat final après ces ajouts : 17 tests audio, 2 géométriques et 4 de session réussis sur JVM ; 22 tests communs réussis sur le variant Android. Les huit impacts du WAV sont reconnus une seule fois, avec une estimation chacun ; tolérance de test ±12 cents et détection ±10 ms. Résultats pré-FIR : 178,00097 / 184,99852 / 192,00144 / 184,99852 / 178,00097 / 192,00144 / 184,99852 / 192,00144 Hz ; erreur maximale absolue 0,014 cent. Temps **dans le signal injecté**, attaque → détection +5 ms, attaque → résultat +320 ms. Ces chiffres ne mesurent pas la latence téléphone, les files UI ni la charge du rendu.

Tests déterministes : frappe simple/décroissance, secondaire inharmonique 311 Hz plus fort mais amorti, secondaire 378 Hz proche du double et amorti, octave exacte 185/370 Hz avec composante basse visible, modes persistants 185/263 Hz rejetés, 317 Hz éloigné de la cible affichée, silence, bruit pseudo-aléatoire à graine fixe, saturation, chunking 127/1024 identique, retour détection séparé du résultat, deux frappes séparées par un retour au calme ; compléments 44,1 kHz réel, anti-repliement et absence de sous-harmonique inventé, démarrage/réinitialisation au milieu d’une décroissance puis nouvelle frappe après le calme.

Le principal a vérifié AudioRecord/rendu et le cycle de vie en émulation puis sur Pixel 9 Pro XL / Android 17, avec des frappes réelles sur caisse claire 14″. Voir [ESSAI-ANDROID.md](ESSAI-ANDROID.md). Un vrai tom et un iPhone physique restent à tester. Restent à vérifier : build natif, permission refusée puis accordée, interruption téléphonique, changement de route, sortie/arrière-plan, témoin micro éteint après libération, plusieurs reprises sans capture doublée, rendu et sélection simultanés, latence filmée impact/retour/Hz, erreurs de résonance, rejets et frappes manquées. Consigner appareil/OS, diamètre, peau opposée étouffée, distance et orientation, pièce, méthode de référence et erreurs en cents séparément des rejets. Ne pas conclure la fiabilité physique à partir des seuls tests JVM.

Références API consultées : [AudioRecord](https://developer.android.com/reference/android/media/AudioRecord), [AVAudioEngine — Apple WWDC](https://devstreaming-cdn.apple.com/videos/wwdc/2014/502xxvo7vov799k/502/502_avaudioengine_in_practice.pdf).
