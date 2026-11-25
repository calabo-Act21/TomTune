# Session physique unique — mesure près d’une vis

**Session réalisée le 25 novembre 2026 :** voir le [rapport physique et ses limites](ESSAI-PHYSIQUE-2026-11-25.md). Les deux premières séries ne se sont pas stabilisées ; un complément demandé ensuite confirme ponctuellement 490,7 Hz à la 9e détection, sans lever le problème de sélection variable. Le ressenti d’une sensibilité à l’intensité est consigné. Aucun tom disponible. La procédure ci-dessous est conservée pour relier les consignes aux observations, sans demander une nouvelle session.

Utiliser **un seul APK debug final** de la livraison, dont l’empreinte et le commit embarqué sont relevés. Mise à jour `adb install -r`, même identifiant `com.tomtune.app`, même certificat debug ; ne pas désinstaller. Le lancement normal doit utiliser le micro et ne montrer aucun bandeau « PCM injecté ». Aucun PCM n’est écrit sur disque.

## Commencer sur la caisse claire 14″

1. Timbre débrayé, peau de résonance étouffée. Choisir une seule vis physique et placer la baguette à **2,5 cm du cercle**. Fixer le téléphone ; noter sa distance, l’orientation et le bruit ambiant.
2. Depuis la fiche d’un tom, ouvrir une session temporaire de diagnostic. Le guidage produit reste réservé aux toms : ne pas appliquer à la caisse claire les conseils tirés de la cible de ce tom. Garder la même vis sélectionnée à l’écran et **ne pas appuyer sur « Suivant »** pendant les deux séries.
3. Jouer **10 coups** à la baguette au même point, environ **toutes les deux secondes**, en comptant réellement les coups. Relever pour chacun détection, rejet et motif, Hz provisoires, confirmation éventuelle et nombre de coups avant **« Mesure stable »**. La vis reste sélectionnée et le micro reste actif après la confirmation.
4. Refrapper un peu plus franchement, toujours sur la même vis, sans déplacer le micro. Chaque fréquence exploitable s’affiche en grand. Si elle contredit la série, la nouvelle lecture est provisoire, la dernière valeur stable reste en dessous et « Suivant » est désactivé. Une nouvelle série stable remplace le brouillon précédent. Si les deux modes sont ambigus, la frappe est non concluante et ne compte pas.
5. Après stabilisation, appuyer une fois sur **« Suivant »** : seule cette action valide la mesure et avance d’une seule vis. Abandonner ensuite le diagnostic temporaire pour éviter toute mesure globale ou référence sur cet instrument.

## Métadonnées pendant cette même session

Si le Pixel est relié en USB au Mac, lancer les commandes depuis le dépôt. Les scripts ne manipulent que les métadonnées logcat, aucune donnée audio. Remplacer `SERIAL` par le Pixel explicite, jamais l’émulateur.

```sh
python3 tools/capture_lug_session.py --adb /private/tmp/tomtune-tools/android-sdk/platform-tools/adb --serial SERIAL --output /private/tmp/tomtune-dix-coups.log --count 10 --interval 2
python3 tools/summarize_lug_trace.py /private/tmp/tomtune-dix-coups.log
python3 tools/capture_lug_session.py --adb /private/tmp/tomtune-tools/android-sdk/platform-tools/adb --serial SERIAL --output /private/tmp/tomtune-coups-francs.log --count 5 --interval 2
python3 tools/summarize_lug_trace.py /private/tmp/tomtune-coups-francs.log
```

Le script annonce les coups après un délai de 3 secondes. Chaque créneau attendu s’étend de −1 à +1 seconde autour du top. `MissedExpected` signifie **aucune détection dans ce créneau**, à qualifier d’impact manqué seulement si l’opérateur confirme avoir frappé à ce top. Un coup décalé, un top omis, un changement de contexte ou des doubles détections exigent une annotation ; aucun nombre de coups physiques n’est inventé. Le résumé distingue `Detected`, `Rejected`/motif, `Estimate`/Hz, `SeriesReset`/motif, `Confirmed`/Hz/compte et `Next`. Sans série comptée, seules les détections observées sont comptées.

Le log debug normal porte `input=microphone`. `input=injected` et le bandeau PCM sont réservés à l’émulateur. Le compteur de série est borné aux cinq dernières valeurs ; le **nombre de coups avant la première stabilité** se lit dans l’ordre des détections, rejets compris, plutôt que dans ce compteur après affinage.

## Si un tom est disponible pendant cette même session

Sur un vrai tom, contrôler aussi le geste guidé près d’une vis sur la peau de frappe puis sur la peau de résonance : étouffer la peau opposée, vérifier le repère, les Hz, le maintien sur la même vis et l’avancement par un seul appui sur « Suivant ». Ce complément reste local ; aucune nouvelle recette du centre ou des références n’est demandée.

## Relevé physique

| Donnée | Relevé réel |
|---|---|
| Pixel / Android / APK / sourceCommit | À relever |
| Instrument / peau / timbre / nombre de vis | Caisse claire 14″, compléter ; timbre débrayé |
| Position du micro / distance / bruit / point à 2,5 cm | À relever |
| Série 1 : coups réellement joués / détections / manqués / rejets | 10 demandés ; résultats à relever |
| Série 1 : Hz par coup / coups avant « Mesure stable » | À relever |
| Série 2 plus franche : coups / rejets / Hz / reconfirmation | À relever |
| Mode ambigu : retour non concluant et absence de comptage | À relever si rencontré |
| Aucun avancement avant appui ; un seul après appui | À relever |
| Tom disponible et geste des deux peaux | À relever, sinon non réalisé |

À ce stade, le retour utilisateur disponible concerne l’**ancien APK** : frappes souvent répétées et lectures environ 350–700 Hz. Aucune série de dix coups avec le nouvel APK n’a encore été observée. Les injections et l’émulateur ne remplacent pas ce relevé.
