# Comparaison près d’une vis — préparation du 26 novembre 2026

**Diagnostic prêt ; aucun nouvel essai sur le Pixel.** À la demande de l’utilisateur, la session physique attend son feu vert. Le bon mode de la caisse claire reste à identifier. Aucun correctif de suivi entre frappes n’est activé dans le produit.

Départ : branche `codex/lot2-3-android`, rapport `7750a22`. L’APK de l’essai précédent provenait de `f19bcbb5e3ec1f1b1981ed29d056d379674c7b57`. Lire aussi [l’essai précédent](ESSAI-PHYSIQUE-2026-11-25.md) : la confirmation ponctuelle à 490,7 Hz à la **9e détection** ne prouve ni neuf coups physiques établis ni la justesse du mode.

## Un seul APK, trois configurations

Le build debug compare automatiquement les configurations pendant la mesure près d’une vis. Le réglage **current** reste seul responsable de l’affichage et de la série produit. Le centre n’est pas instrumenté par cette comparaison. Aucun écran de guidage de caisse claire n’est ajouté.

| Identifiant de trace | Bande | Déclenchement RMS | Sélection |
|---|---|---|---|
| `lot0` | 80–700 Hz | 0,018 | Politique historique, y compris choix grave d’une famille harmonique |
| `current` | 55–900 Hz | 0,010 | Politique locale de `f19bcbb`, avantage de persistance ≥ 2,5 entre concurrents |
| `band80_700` | 80–700 Hz | 0,010 | Identique à `current`, **seule la bande change** |

Le lot 0 est reproduit avec ses paramètres et sa politique dans le même calcul partagé, vérifié événement par événement contre la copie figée de `b519e98` en test, y compris la règle harmonique 350/700. Cette règle historique n’est pas appliquée au produit local. Pas de choix par cible, de rabattement d’octave ou de verrouillage du premier pic dans la nouvelle instrumentation. Les seuils de stabilité restent **3 valeurs / 8 cents ou 5 / 18 cents**, médiane, fenêtre de cinq valeurs et remise à 1 en cas de contradiction. Seul **« Suivant » manuel** accepte et avance.

La capture Android unique fournit les mêmes blocs PCM aux trois détecteurs sur le worker ordonné. Les événements natifs gardent leur propre onset. Les déclenchements distants de ≤ 40 ms sont associés à un identifiant d’impact diagnostic ; ce regroupement temporel n’est pas une preuve de coup physique unique. Un déclenchement plus tardif reste un autre groupe, à examiner avec les tops.

Pour chaque groupe, une seconde passe `phase=paired` analyse **les mêmes 320 ms**, depuis le plus ancien onset du groupe, avec les trois configurations. Ainsi :

- `NativeDetected` et `CMP Impact … lot0=missed` documentent les différences de **détection** ; les ID natifs ne sont pas assimilés entre configurations.
- `phase=native` décrit la sortie réellement obtenue avec chaque déclencheur.
- `phase=paired` isole le **choix après détection**, sans décalage de fenêtre et même si l’un des détecteurs n’a pas déclenché. Cette sortie ne compte pas comme une détection ou une lecture produit.
- La clé est `run + generation + impact + frame + rate`. `frame/rate` est l’horodatage PCM du groupe ; chaque onset natif est également conservé.

Le tampon de comparaison est borné à 400 ms après chaque bloc, en plus des captures natives de 320 ms. Le PCM reste en mémoire sur l’appareil. Aucun export audio, WAV ou sérialisation de samples n’est ajouté.

## Contenu et limites de la trace

Tag logcat `TomTuneTrace`, build debuggable seulement ; trois chemins de calcul, pas trois microphones.

- `NativeDetected` : RMS et crête sur 5 ms, bruit estimé et enveloppe précédant l’attaque.
- `Attack` : RMS/crête des 20 premières ms, fraction saturée. Niveaux PCM normalisés, **pas une force de frappe ni un niveau SPL étalonné**.
- `Window` : fenêtres Hann de 128 ms, à +20/+84/+148 ms ; concentration et pics sous la forme `Hz,puissanceFFT,dBRelatifs,éligibleAuSélecteur`.
- La sélection conserve ses six pics maximum, plancher relatif 0,035 en puissance (≈ −14,56 dB). L’observation debug descend seule à 0,001 (−30 dB), douze pics maximum. Elle n’alimente ni la concentration, ni le choix, ni la série. Un pic absent peut être sous ce plancher, hors bande, non résolu ou hors des douze pics : ne pas conclure automatiquement à sa disparition acoustique.
- `Mode` : association à ±35 cents entre fenêtres, fréquences/puissances et décroissance en dB depuis la première fenêtre ; `null` explicite les absences observées. `eligible` indique la persistance admise par la sélection. Ces associations décrivent **une frappe** ; aucun suivi acoustique inter-frappes n’est encore imposé.
- `Selection` : fréquence/confiance ou rejet `Noise`, `Clipped`, `AmbiguousModes` ; règle et avantage de persistance si applicable.
- Produit : `CaptureStart/Stop`, `CaptureIssue`, `SeriesReset`, `DebugReset`, `Confirmed`, transitions `UI readyForNext/stableHz/feedback/count/listening`, appui `Next`. Les états sont tracés à chaque mutation, avant confluence de l’affichage.
- Microphone : `MARK CLOCK` associe le frame zéro AudioRecord à l’horloge monotone. Tops `MARK command=cue`, début/fin et remises à zéro portent aussi l’horloge monotone et l’époque. Les journaux logcat ont leur propre date d’émission, à distinguer du temps PCM.

L’union des détecteurs ne peut révéler un impact manqué par les trois. Les tops et les déclarations de l’opérateur sont nécessaires pour établir les coups réellement joués.

## Résultats injectés, distincts du physique

Fixture [ModeComparisonFixture](../../shared/src/commonMain/kotlin/com/tomtune/audio/ModeComparisonFixture.kt), PCM mono en mémoire, 20 impacts connus, à 2 s d’intervalle PCM, fréquences **491/699/801 Hz**. Dix formes répétées avec gains PCM 0,11 puis 0,70 : pics dominants alternés, trois modes équilibrés, puis décroissances différenciées (7/18/22 s⁻¹). Les deux niveaux sont synthétiques, sans équivalence revendiquée avec deux forces physiques.

| Bloc synthétique de 10 | Configuration | Détections natives | Lectures natives acceptées | Rejets natifs | Première stabilité avec série remise à zéro au bloc |
|---|---|---:|---:|---|---|
| Niveau faible | lot 0 | 7 | 4 | 2 ambigus, 1 Noise | Aucune |
| Niveau faible | actuel | 10 | 8 | 2 ambigus | Aucune |
| Niveau faible | actuel, bande 80–700 | 10 | 6 | 2 ambigus, 2 Noise | Aucune |
| Niveau fort | lot 0 | 10 | 6 | 2 ambigus, 2 Noise | Aucune |
| Niveau fort | actuel | 10 | 8 | 2 ambigus | Aucune |
| Niveau fort | actuel, bande 80–700 | 10 | 6 | 2 ambigus, 2 Noise | Aucune |

La fixture change volontairement le mode dominant ; elle ne constitue pas un scénario de répétabilité attendu. Les trois non-détections historiques au niveau faible sont les coups synthétiques 1–3. Sur leurs fenêtres communes, le lot 0 sélectionne 491 et 699 aux coups 1–2 : cela distingue une **non-détection native** d’un **échec de sélection**. Au coup 3, dominé par 801, la bande historique rejette `Noise`.

Sur les deux blocs, l’actuel accepte environ **491/699/801/491/491/699/699/801 Hz** aux coups 1/2/3/5/6/7/8/9 ; les coups 4 et 10 sont ambigus. La bande réduite conserve les 491 et 699, mais rejette les coups 3 et 9 dominés par 801. Les analyses appariées donnent 16 lectures actuelles et 12 pour chacune des bandes 80–700 sur les 20 impacts. **Réduire la bande seule n’identifie pas le mode pertinent.**

Constats limités à cette injection :

1. Avec amplitudes déséquilibrées et même décroissance, un concurrent peut être observable sous le plancher de sélection ; un autre mode devient le seul candidat éligible. L’alternance de lecture peut donc exister sans déplacement des fréquences injectées.
2. Avec plusieurs modes éligibles de décroissance similaire, le sélecteur s’abstient (`AmbiguousModes`). Avec décroissance suffisamment différenciée, l’avantage de persistance tranche. Les traces montrent les niveaux par fenêtre et les candidats perdus/observables.
3. Le seuil historique manque des attaques faibles présentes dans le flux. La comparaison en fenêtres communes évite d’en attribuer la cause au choix de fréquence.
4. Le retour utilisateur sur l’intensité reste une **hypothèse physique**, non démontrée par ces signaux artificiels. Aucun des trois Hz n’est désigné comme la bonne résonance de sa vis.

Preuves : [trace 48 kHz](preuves/comparaison-vis/injected-48000.log), [résumé par bloc 48 kHz](preuves/comparaison-vis/injected-48000-summary.json), [trace 8 kHz](preuves/comparaison-vis/injected-8000.log), [résumé 8 kHz](preuves/comparaison-vis/injected-8000-summary.json). Les mêmes comptes sont obtenus aux deux taux. Le test affirme l’égalité exacte des événements produit avec/sans comparaison.

## Session Pixel unique, prête mais non lancée

**Deux blocs physiques : non réalisés.** Aucun compte, Hz, taux de détection ou résultat de répétabilité physique nouveau. Ne pas confondre les deux niveaux injectés ci-dessus avec les blocs légers/francs du Pixel.

Après feu vert, installer uniquement l’APK identifié dans `livraison.json`, vérifier le SHA-256 du `base.apk` installé et son `assets/build-info.properties`. Garder le kit et les références inchangés. L’instrument physique est la **caisse claire 14″**, sur une seule vis ; le tom affiché dans l’application sert seulement de support diagnostic. Consigner son libellé exact, sans utiliser sa cible pour trancher.

Préparation à consigner : timbre débrayé ; étouffement **fixe et reproductible** de la peau opposée, description précise de l’objet/contact (éviter une pression de main variable) ; même baguette ; même point à environ **2,5 cm du cercle** ; même vis ; même distance/orientation du micro. Noter bruit ambiant, éventuel changement de dispositif et lecture d’un accordeur de batterie indépendant s’il est disponible. Ne pas modifier l’accordage pendant ces deux blocs.

1. Ouvrir la même vis et laisser le micro actif. Ne pas appuyer sur « Suivant » pendant les coups.
2. Remise à zéro debug : nouvelle génération, série vide, brouillon stable effacé, « Suivant » indisponible ; même vis et aucune écriture de mesure/référence. Le journal doit le confirmer.
3. Cinq secondes de silence ; marqueur de début ; **10 tops espacés de 3 s**, un coup léger par top ; cinq secondes de silence ; marqueur de fin. Noter les rangs réellement joués, les coups supplémentaires/tardifs/omis/incertains.
4. Nouvelle remise à zéro vérifiée ; même montage ; même protocole pour **10 coups un peu plus francs**. Recueillir les observations séparément.
5. Relire les confirmations et la disponibilité réelle de « Suivant ». Si un appui de contrôle est fait après les deux blocs, le dater séparément ; il ne fait pas partie des frappes du diagnostic. Abandonner sans enregistrer de référence et vérifier l’arrêt micro/le snapshot du kit.

Commande préparée, **non exécutée sur le Pixel** :

```sh
python3 tools/capture_mode_comparison.py --adb CHEMIN_ADB --serial PIXEL --output DOSSIER_METADATA
python3 tools/summarize_mode_comparison.py DOSSIER_METADATA/metadata.log
```

Le script demande un départ explicite pour chacun des deux blocs et des annotations opérateur après chaque bloc. Il ne déduit pas les coups joués des tops. Les marqueurs debug sont disponibles uniquement dans l’activité d’un APK debuggable. Le résumé associe les impacts aux tops par horloge AudioRecord/monotone, fenêtre déclarée −0,15 à +1,20 s ; le hors-créneau reste séparé. Une attribution à un créneau ne prouve pas un coup réel. Tout manque de `CLOCK` rend cette attribution indisponible. Les déclarations sont conservées dans `operator.json`, à confronter aux détections, rejets, lectures et confirmations.

Critère proposé pour une éventuelle correction : **stable au plus tard au 5e coup réellement joué de chaque bloc, même mode suivi, aucune confirmation concurrente**. Rapporter séparément coups joués établis/incertains, détections en/hors créneau, multi-détections possibles, créneaux joués sans détection, rejets et lectures acceptées. Sans comparateur indépendant, conclure seulement sur la répétabilité ; **justesse ouverte**. La validation physique du parcours sur un vrai tom reste distincte.

## Décision et hypothèse suivante

Aucune correction acoustique choisie avant les traces physiques. L’instrumentation permet maintenant de distinguer : (a) un mode présent mais perdant ou sous le seuil d’éligibilité, (b) un mode qui cesse d’être observable dans certaines fenêtres, (c) un déclenchement absent, tardif ou hors créneau.

Si les deux blocs montrent le même mode résolu de manière cohérente, l’étape suivante sera un suivi entre frappes fondé sur sa fréquence et sa persistance observées sur plusieurs impacts, **sans verrouiller la première lecture et sans cible d’accordage**. Tester alors explicitement l’abstention quand le mode n’est plus observable ou quand plusieurs trajectoires restent plausibles, ainsi que l’absence de confirmation d’un concurrent. Le mode à suivre ne peut pas encore être choisi à partir du rapport physique précédent ou de cette fixture. Si les traces ne suffisent pas, livrer le bilan et tester ensuite le fenêtrage/déclenchement ou un montage plus reproductible selon le défaut observé.

## Vérifications et livraison

- 51 tests JVM, 43 tests unitaires shared Android, 2 tests unitaires app Android ; aucun échec final. [Comptes](preuves/comparaison-vis/tests-counts.json), [build et tests](preuves/comparaison-vis/build-tests-android.log).
- Tests nouveaux : comparaison 20 impacts à 8/48 kHz, identité des événements produit, équivalence historique, reset/discontinuité, remise à zéro d’un brouillon stable, invalidation des événements tardifs et conservation de la référence.
- Émulateur API 36, PCM injecté seulement : confirmation, « Suivant » manuel et reset du brouillon ; comparaison Android des 20 impacts et absence d’avancement automatique. [Recette ciblée](preuves/comparaison-vis/emulateur-comparaison.log).
- [Vérification du parseur](preuves/comparaison-vis/check-metadata.log) : distinction natif/apparié, association PCM/top monotone, hors créneau séparé. Aucune inférence de coup physique.
- Une première recette a échoué sur les libellés anglais de l’émulateur ; langue de TomTune corrigée en français. L’ancienne recette complète a ensuite passé la partie vis/contradiction/« Suivant », puis échoué sur une assertion d’un bouton central hors vue, au-delà du périmètre demandé. Journaux conservés ; la recette ciblée évite cette vérification centrale.
- Gradle requiert ici une exécution hors sandbox pour ses sockets locales ; l’échec initial est conservé dans `preflight-sandbox.log`. JDK 17, SDK/émulateur API 36 restaurés dans `/private/tmp/tomtune-tools`. Aucun accès au Pixel.

Commandes :

```sh
./gradlew :shared:jvmTest :shared:testDebugUnitTest :composeApp:testDebugUnitTest :composeApp:assembleDebug --console=plain
python3 tools/verify_mode_comparison_android.py --adb /private/tmp/tomtune-tools/android-sdk/platform-tools/adb --output docs/lot2-3/preuves/comparaison-vis/emulateur-comparaison
python3 tools/check_mode_metadata.py
python3 tools/summarize_mode_comparison.py docs/lot2-3/preuves/comparaison-vis/injected-48000.log --injected-fixture
# extraction des comptes JUnit XML ; py_compile ; git diff --check ; apksigner ; SHA-256
```

Le seul APK à utiliser est `composeApp/build/outputs/apk/debug/composeApp-debug.apk`. Son commit source exact et son SHA-256 sont consignés dans [l’identité finale](preuves/comparaison-vis/livraison.json), après build depuis un commit propre. Le commit de preuves suivant n’altère pas ses sources. Les nouvelles preuves de cette préparation sont uniquement des métadonnées (journaux, JSON, XML), sans PCM, audio, captures d’écran ni APK. Code et rapport restent sur la branche du lot, sans fusion dans `main`.

APK final : SHA-256 `d52725c1067ae03aa886e52b28737bbffcaf15ab00f4b7c3b0c64bd6d428fe0a` ; source exacte [78ccc89](https://github.com/calabo-Act21/TomTune/commit/78ccc89806ebd899be4c5296818c2226c60d9c5b) ; `worktreeDirty=false` vérifié dans le binaire. [Build final et tests](preuves/comparaison-vis/build-final.log), [empreinte installée sur émulateur](preuves/comparaison-vis/final-install-emulator.log), [signature](preuves/comparaison-vis/signature.log). Le certificat est identique à celui de l’APK `f19bcbb` testé sur le Pixel. La recette ciblée a été rejouée avec ce seul APK final sur émulateur et donne les comptes du tableau ; aucun Pixel utilisé.
