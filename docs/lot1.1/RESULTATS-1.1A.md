# Livraison du pilote 1.1A Android — 24 novembre 2026

## Base et périmètre

Départ propre de `main` à `74922c2` (`prepare lot 1.1`), après `git fetch origin main`. La fusion de la PR #2 est bien présente (`9fa4601`). Branche locale : `codex/lot1.1a-android`. Un seul agent ; aucun push, PR, publication ni lancement manuel de CI.

Lus : `AGENTS.md`, `SPEC.md` (dont V11-01 à V11-04), `LOT-1.1.md`, `CHARTE.md`, `PARCOURS.md`, index et carte de zones des pilotes. Les deux PNG et leur manifest sont présents et embarqués depuis les originaux de `docs/visuels/lot1.1`.

Arrêt à **1.1A**. Les sept anciens visuels de kit et les autres photos de fûts restent utilisés. La géométrie historique du schéma zéro floor est conservée. Aucun changement du stockage, de la création/édition/sélection métier, du contrôleur de navigation, de l’audio, de l’accordage ou de l’entrée iOS.

## Livraison

- Palette studio regroupée : fond `#F3EBE3` jusque derrière les barres système, texte `#261B19`, bordeaux `#752C3E`, menus `#FFF9F3`. Commandes en zone sûre, défilement avec texte agrandi.
- Kit regroupé en haut sous le sélecteur/menu : 8 dp sous les commandes et sur les côtés, ajustement proportionnel, silhouette et ombres entières. Le rectangle réellement dessiné sert aussi aux touchers et aux limites accessibles.
- Pilote `r3-f2` : polygones peau/coque lus depuis le manifest, résolution dans `hitOrder`. Les rectangles accessibles ne capturent pas les touchers ordinaires : la résolution polygonale commune décide du fût visible au premier plan. Un seul élément accessible par ID ; ordinal pour les doublons de type/diamètre.
- Chargement : publication unique d’une frame contenant le kit immuable, le bitmap et les zones déjà associées aux IDs. Chargement sans cibles, filtrage immédiat des frames d’un autre kit, annulation et compteur de génération contre les réponses obsolètes. Une erreur publie le repli schématique du kit demandé avec ses propres fûts, y compris les floors.
- Fiche : titre déroulant 24 sp, ordre visuel du kit, choix courant marqué, remplacement sur place via le contrôleur existant. Rack 12″ / six vis détouré. Carte répétant le diamètre retirée ; nombre de vis et mention illustrative conservés, données seules hors catalogue ou si une image échoue. Pas de commandes d’accordage ajoutées.
- Quatre nouvelles chaînes dans chacun des catalogues EN, FR, ES et DE. Valeurs stockées et IDs indépendants de la langue.

## Vérifications et commandes

Outillage local préexistant :

```sh
export JAVA_HOME=/private/tmp/tomtune-tools/jdk-17.0.21.1+2/Contents/Home
export GRADLE_USER_HOME=/private/tmp/tomtune-gradle
export ANDROID_HOME=/private/tmp/tomtune-tools/android-sdk
```

| Commande / contrôle | Résultat |
|---|---|
| `git fetch origin main`, `git log -6 --oneline --decorate`, `git switch -c codex/lot1.1a-android` | Main à jour, fusion #2 vérifiée, branche créée. |
| `./gradlew -Pmobile=false :shared:jvmTest --tests 'com.tomtune.kit.*' --console=plain` | **8 tests réussis** : 5 existants et 3 tests ciblés présentation. |
| `./gradlew -Pmobile=false :shared:jvmTest --tests 'com.tomtune.kit.KitPresentationTest.lateDecodeCannotReplaceCurrentImageOrIdsEvenForSameVisualKey' --console=plain` | **Réussi** après renforcement du cas tardif : la première requête reste volontairement active, revient après B et ne peut remplacer ni son image ni ses IDs, même à composition identique. Seul ce test a été relancé. |
| `./gradlew :composeApp:assembleDebug --console=plain` | **BUILD SUCCESSFUL**. Builds ciblés après rétablissement exact de la géométrie zéro floor puis ajout de la coche visible du fût courant. Build final : **BUILD SUCCESSFUL**, 3 s. |
| `python3 tools/check_lot11_assets.py --apk composeApp/build/outputs/apk/debug/composeApp-debug.apk` | **Réussi** : anciens assets inchangés, 47 clés de texte dans les quatre langues, deux PNG RGBA, dimensions, sept polygones ordonnés et octets embarqués identiques aux sources. |
| `git diff --check` | Réussi. |
| `shasum -a 256 composeApp/build/outputs/apk/debug/composeApp-debug.apk` | Voir [l’empreinte de l’APK](preuves/apk.sha256). |
| ADB `-s emulator-5554 install -r`, `shell am start -W -n com.tomtune.app/.MainActivity`, `uiautomator dump`, `input tap`, `input keyevent`, `exec-out screencap -p` | Simulation Android ciblée, journaux et captures dans `preuves/`. Helpers temporaires : `/private/tmp/tomtune-lot11-ui.py` et `/private/tmp/tomtune-lot11-final.py`. |

Le bac à sable bloque les sockets de Gradle/ADB et l’écriture de `.git`. Les commandes correspondantes ont réussi après escalade autorisée. Un premier démarrage de l’émulateur a nécessité `ANDROID_AVD_HOME=/private/tmp/tomtune-tools/android-user/avd` explicite. Les helpers ont été adaptés : libellés Compose portés par un enfant du bouton schématique ; comparaison des données hors compteur de révision après les éditions explicites 6→7→6. Les révisions augmentent normalement à chaque enregistrement. Pour le contrôle final du menu, le champ Android attendu est `checked` (export de `selected` par Compose), et les changements de police/retraits de menu doivent finir avant le geste suivant. Les assertions ont été corrigées et les seules étapes restantes reprises. Ces échecs de helper ne sont pas des erreurs de l’application ; le journal de clôture confirme les résultats et les réglages restaurés. Avertissements `expect`/`actual` préexistants uniquement.

Les tests de présentation injectent un décodage différé/échoué ; ce ne sont pas des essais micro. Les essais ADB sont une **simulation sur émulateur Android 15 / API 35 ARM64**, distincte d’un téléphone physique. Aucun PCM injecté, aucune capture audio, aucun essai acoustique ni build iOS. Les tests antérieurs sont conservés ; aucune suite audio relancée.

## Critères P1 à P7

La validation finale sur téléphone appartient au propriétaire. Aucun de ces critères n’est déclaré physiquement validé dans cette livraison.

| ID | Vérification technique | À vérifier sur le téléphone |
|---|---|---|
| P1 | Captures 1080 × 2400 et 1200 × 1920 : fond continu, kit directement sous le bandeau, pédale et pieds entiers. Comparaison avec la capture historique du lot 1 sur le même émulateur. Essai des détourages sur blanc et cyan : pas de cadre opaque ni d’ombre visiblement coupée. | Rendu des contours et des ombres, cadrage et barres système avec le mode de navigation du téléphone. |
| P2 | Rack 12″ / six vis : silhouette et ombre entières, titre et nombre de vis visibles, diamètre une seule fois ; captures inspectées aux deux rapports d’écran. | Taille utile, détails chromés et lisibilité à distance d’utilisation. |
| P3 | Deux touchers (peau et coque/face) sur chacun des sept fûts ouvrent la même fiche ; point de chevauchement normalisé `(0.69, 0.50)` ouvre le floor 18″ au premier plan. Toucher de coque rack 12″ réussi après passage à 1200 × 1920 ; transformation avec décalage testée en Kotlin. | Toucher les bords et les petites portions visibles, vérifier la précision tactile réelle. |
| P4 | Tests : aucune frame pendant le chargement, réponse A tardive ignorée après B à même clé visuelle, erreur → kit B sans ancien artwork ; transformation et priorité de polygones couvertes. Deux allers-retours Studio/Live sur émulateur : image et IDs corrects, schéma zéro floor utilisable. Création UI d’un kit à un rack / zéro floor, ouverture de son rack puis resélection de Studio réussies ; les deux kits précédents gardent leurs données, hors révisions attendues des éditions explicites. | Enchaîner les sélections rapidement, y compris entre compositions identiques, et vérifier zéro floor. Le retard forcé est testé dans le chargeur Kotlin, pas artificiellement dans l’APK. |
| P5 | Trois choix successifs via le titre remplacent la fiche. Android Retour et « Mon kit » reviennent au même kit en un geste. JSON inchangé par ces navigations. | Refaire ces gestes sur le téléphone. |
| P6 | Fiche rack 12″ / sept vis hors catalogue relancée avec texte 200 % : données, retour et sept entrées de liste lisibles ; captures inspectées. Sémantique accessible et choix courant unique coché vérifiés dans le XML UIAutomator ; coche visible à texte 200 %. TalkBack non activé. | TalkBack, parcours de focus, agrandissement et zones sûres avec les réglages habituels du téléphone. |
| P7 | Mode avion, force-stop, relance : images présentes, kits et sélection identiques au snapshot avant arrêt sur l’émulateur. Dernier APK installé avec `-r` puis relancé hors ligne : trois kits et Studio actif préservés. | Mise à jour `-r` sur le téléphone puis mode avion, arrêt forcé et relance avec les kits existants. |

Preuves : [parcours peau/coque, navigation et mode avion](preuves/emulateur-parcours.log), [essais du second écran et texte agrandi](preuves/emulateur-ecrans.log), [création et zéro floor](preuves/emulateur-creation.log), [dernière installation hors ligne](preuves/emulateur-final.log), [clôture après adaptation des helpers](preuves/emulateur-cloture.log), [état coché du sélecteur](preuves/emulateur-selecteur.xml), [tests des kits](preuves/tests-kits.log), [test tardif renforcé](preuves/test-decodage-tardif.log), [build final](preuves/build-debug-final.log), [assets](preuves/controle-assets.log).

Comparaison : [kit avant, lot 1](../lot1/preuves/emulateur-kit-3-2.png), [kit pilote 1080 × 2400](preuves/emulateur-kit-1080x2400.png), [kit pilote 1200 × 1920](preuves/emulateur-kit-1200x1920.png), [rack pilote](preuves/emulateur-rack-12-6.png), [hors catalogue à texte 200 %](preuves/emulateur-hors-catalogue-texte-200.png), [sélecteur à texte 200 %](preuves/emulateur-selecteur-texte-200.png), [détourages sur blanc et cyan](preuves/alpha-fonds-essai.png). Les fonds blanc/cyan sont des compositions de contrôle réalisées hors application ; les PNG source n’ont pas été modifiés.

## APK à installer

Chemin exact :

```text
/Users/camlaborde/Workspace/TomTune/TomTune/composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

```sh
/private/tmp/tomtune-tools/android-sdk/platform-tools/adb install -r "/Users/camlaborde/Workspace/TomTune/TomTune/composeApp/build/outputs/apk/debug/composeApp-debug.apk"
```

`-r` conserve les données. Si plusieurs appareils sont connectés, ajouter `-s SERIAL` avant `install` pour cibler le téléphone. Ne pas désinstaller l’ancienne version pour cette validation de persistance.

Le lot **1.1B reste en attente** de validation des deux pilotes et de fourniture des autres visuels détourés et de leurs cartes.

## Installation sur le téléphone du propriétaire

Le 24 novembre 2026, après connexion du Pixel 9 Pro XL, l’APK final a été installé avec `adb -s PHONE_SERIAL install -r` (**Success**), puis ouvert avec `am start -W` (**Status: ok**, démarrage à froid). Aucune désinstallation ni remise à zéro. Voir [le relevé d’installation et l’empreinte](preuves/installation-telephone.log). Cette installation ne vaut pas validation physique de P1 à P7 ; les gestes et le rendu restent à confirmer par le propriétaire.

## Révision après essai physique du propriétaire

Le propriétaire indique avoir pu valider le pilote sur son téléphone, puis demande deux corrections de présentation : reprendre la couleur des images d’origine et centrer le kit verticalement. Il précise ensuite de conserver **le taupe grisé sur tous les écrans**. Cette demande remplace le fond uniforme `#F3EBE3` et le regroupement du kit en haut décrits dans la livraison initiale ci-dessus. Le retour constitue une validation physique globale déclarée ; aucun relevé individuel supplémentaire de P1 à P7 ou de TalkBack n’est fourni.

- Fond commun : dégradé Compose plein écran `#AA9E99`, `#B5A8A3`, `#BCAFAA`, `#BFB3AE`, `#BCB0AD`, mesurés sur les bords libres de l’image de kit originale. Il reprend ses tons ; la texture photographique n’est pas recopiée. Le fond s’étend derrière les barres système sur tous les écrans du parcours kit.
- Bordeaux `#602033` et texte secondaire `#42332E` assombris pour préserver la lisibilité sur le taupe. Menus clairs conservés. Aucun texte ajouté ni changement métier.
- Kit centré verticalement dans l’espace restant sous le bandeau et au-dessus de l’aide. Les hauteurs effectives du bandeau et de l’aide sont mesurées, y compris lorsque le texte grandit. La marge latérale reste de 8 dp. Le minimum de hauteur permet de défiler si l’écran devient trop petit. Le bitmap et les zones utilisent toujours le même rectangle ajusté.
- `CHARTE.md` mise à jour avec cette décision du propriétaire. Le travail reste dans le pilote 1.1A.

Vérifications ciblées :

| Commande / essai | Résultat |
|---|---|
| `./gradlew :composeApp:assembleDebug --console=plain` avec les mêmes variables d’environnement | **BUILD SUCCESSFUL**, 2 s, [journal final après précision sur le fond commun](preuves/build-debug-retours-final.log). Un premier build de cette révision précédait la précision « taupe pour tous les écrans ». |
| `python3 /private/tmp/tomtune-lot11-retours.py` ; ADB explicitement ciblé sur `emulator-5554` | **Réussi** : centrage mesuré à 0,15 px près en 1080 × 2400 et 0,27 px en 1200 × 1920 ; toucher de coque rack 12″ ouvre la bonne fiche après déplacement, retour au même kit et JSON inchangé. [Journal](preuves/emulateur-retours.log). |
| `python3 tools/check_lot11_assets.py --apk composeApp/build/outputs/apk/debug/composeApp-debug.apk` | **Réussi** : [assets et ressources](preuves/controle-assets-retours.log). |
| `git diff --check` | Réussi. Les tests existants sont conservés ; aucune suite métier/audio relancée pour ces seuls changements de présentation. |
| `adb -s PHONE_SERIAL install -r …`, puis `am start -W -n com.tomtune.app/.MainActivity` | **Success** sur le Pixel 9 Pro XL. Le fichier des kits est strictement identique avant/après mise à jour. [Relevé et empreinte](preuves/installation-telephone-retours.log). La validation visuelle de cette nouvelle révision reste à faire par le propriétaire. |

Captures de simulation inspectées : [kit 1080 × 2400](preuves/emulateur-retours-kit-1080x2400.png), [kit 1200 × 1920](preuves/emulateur-retours-kit-1200x1920.png), [fiche sur taupe](preuves/emulateur-retours-rack-1080x2400.png). Une capture avant mise à jour du téléphone était noire (affichage indisponible) et a été écartée ; aucune comparaison physique avant/après n’est donc revendiquée.

L’APK révisé conserve le même chemin et la même commande d’installation que ci-dessus. Son empreinte courante figure dans `preuves/apk.sha256`.

## Dernier choix de fond — beige rosé commun

Le 24 novembre 2026, le propriétaire préfère finalement la couleur derrière les images de fût seul et demande de remplacer le taupe partout. Le fond commun est désormais le beige rosé relevé sur les bords de `toms/rack-12-6-vis.png` : `#E5D8D2` → `#E7DAD5` → `#E7D9D4`. Cette décision remplace le choix taupe décrit plus haut. Une seule palette s’applique à tous les écrans du parcours kit, derrière les barres système ; le centrage vertical demandé reste en place. La charte est mise à jour.

`./gradlew :composeApp:assembleDebug --console=plain` : **BUILD SUCCESSFUL**, 3 s ; [journal](preuves/build-debug-beige-rose.log). `git diff --check` réussi. Modification de couleur uniquement ; tests existants conservés, sans relance des suites métier/audio. Vérification visuelle ciblée par captures de simulation : [accueil](preuves/emulateur-beige-kit.png) et [fiche](preuves/emulateur-beige-fut.png). L’APK garde le même chemin ; l’empreinte courante est dans `preuves/apk.sha256`. La validation physique de ce dernier choix de couleur reste au propriétaire.

Installation de cette révision sur le Pixel 9 Pro XL : `install -r` **Success**, fichier des kits strictement identique avant/après ; ouverture effective de `MainActivity` à froid, **Status: ok**, 434 ms. [Relevé](preuves/installation-beige-rose.log).

## Validation du code et autorisation de fusion

Le 24 novembre 2026, le propriétaire valide le code dans son dernier état (fond beige rosé commun, kit centré verticalement) et autorise explicitement le push puis la fusion dans `main`. Cette autorisation remplace la consigne initiale d’attendre avant publication. L’identifiant matériel du téléphone dans les commandes et relevés est remplacé par `PHONE_SERIAL` avant publication. Les résultats de build, tests et essais consignés ci-dessus sont conservés ; aucune vérification déjà réussie n’est relancée pour cette seule publication.
