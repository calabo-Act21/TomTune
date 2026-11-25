# Essai physique près d’une vis — 25 novembre 2026

**Une stabilité ponctuelle a été obtenue dans une série complémentaire vers 490,7 Hz, mais la sélection de fréquence reste variable.** Les deux premières séries demandées n’avaient donné aucune confirmation stable. Dans le complément, les premières lectures alternent encore vers 491/699/800 Hz avant la confirmation à la 9e détection. L’utilisateur signale une impression de fortes variations de fréquence lorsqu’il change légèrement l’intensité de frappe. La fiabilité acoustique de la mesure n’est donc pas établie.

## Appareil, instrument et méthode

- Pixel 9 Pro XL, Android 17 / API 37, relié en USB. APK final installé en mise à jour, empreinte vérifiée : `b8f82d4cb1ae4791c7e5ebb9474b94a003b4806d1f72233723eab293a956d878`. Source embarquée : `f19bcbb5e3ec1f1b1981ed29d056d379674c7b57`. Aucun nouvel APK construit pendant l’essai.
- Instrument déclaré : **caisse claire Pearl Decade Maple 14″**, peau de frappe, **8 vis**, timbre débrayé ; peau de résonance étouffée avec la main. Téléphone déclaré à environ **10 cm du centre de la peau**. Petits coups à la baguette, puis coups un peu plus francs. Consigne : même vis, point à **2,5 cm du cercle**, environ un coup toutes les **2 secondes**, téléphone et étouffement fixes. Ces positions sont déclarées/consignées par consigne, sans mesure vidéo indépendante ; bruit ambiant et orientation du micro non caractérisés.
- Support dans l’application relu dans les exports Android : **Floor tom 16″, huit vis, peau de frappe, vis 1**, cible locale affichée **110,1 Hz**. L’utilisateur évoquait un tom 18″ comme support ; le libellé effectivement observé est conservé ici. L’instrument frappé reste la caisse claire. La cible du tom ne sert pas de référence de justesse pour cet essai.
- Capture normale : traces `input=microphone`, aucun bandeau d’injection. **Aucun PCM ni enregistrement sonore sur disque.** Les preuves sont des métadonnées logcat et des états d’écran. Il n’existe donc pas de PCM physique à rejouer avec le lot 0.

## Résultats du premier relevé

| Phase | Compte physique | Détections / estimations / rejets | Stabilité et avancement |
|---|---|---|---|
| Essai libre préliminaire, génération 3 | Nombre de coups inconnu, utilisateur : « pas mal de frappes », petits coups à la baguette | **31 / 23 / 8** ; 7 `AmbiguousModes`, 1 `Noise`. Lectures surtout 595,8–598,6 Hz, avec lectures isolées 801,1 et 258,3 Hz | Première confirmation à la **9e détection**, 597,6 Hz, série de 3 estimations ; dernière confirmation 596,8 Hz. Il ne s’agit pas de neuf coups physiques établis. Aucun `Next`. |
| Après une remise à zéro préparatoire, génération 6 | Non compté | **2 / 0 / 2** ; 1 `Noise`, 1 `AmbiguousModes` | Aucune confirmation. Ces deux événements expliquent le premier bilan oral total de 33 détections / 23 estimations / 10 rejets. |
| Fenêtre brute des dix petits coups, génération 9 | **10 coups confirmés par l’utilisateur**, après le message « Départ », puis arrêt | Fenêtre de 86,236 s, incluant attente et réponse : **27 / 11 / 16** ; 14 `Noise`, 2 `AmbiguousModes` | Aucune confirmation, huit remises à zéro pour incompatibilité, aucun `Next`. Ne pas interpréter les 27 événements comme 27 frappes de l’opérateur. |
| Séquence régulière compatible avec ces dix coups, détections 17–26 | Attribution par cadence et compte déclaré ; correspondance individuelle non synchronisée | **10 / 8 / 2** ; deux `AmbiguousModes`. Dix détections sur **18,260 s**, espacées de 1,880 à 2,140 s environ | Aucune confirmation ; alternance vers **491 / 699 / 801 Hz**. « Suivant » reste désactivé sur vis 1. |
| Coups plus francs, même génération 9, sans remise à zéro ni changement de vis | Utilisateur : **« plus de 5 coups je pense »**, nombre exact inconnu | **9 / 5 / 4** dans la fenêtre ; 3 `AmbiguousModes`, 1 `Noise`. Hz acceptés : **491,909 ; 707,490 ; 492,162 ; 491,954 ; 599,992** | Aucune confirmation, quatre remises à zéro pour incompatibilité. À la fin : **600,0 Hz provisoire**, série à 1, vis 1, « Suivant » désactivé. Aucun `Next`. |

La séquence des dix petits coups retenue **par cadence**, sans prétendre disposer d’annotations physiques individuelles :

| Rang dans la séquence | ID détecté | Résultat |
|---|---:|---|
| 1 | 17 | Rejet `AmbiguousModes` |
| 2 | 18 | 699,571 Hz |
| 3 | 19 | 491,172 Hz |
| 4 | 20 | 491,203 Hz |
| 5 | 21 | 801,058 Hz |
| 6 | 22 | 801,068 Hz |
| 7 | 23 | Rejet `AmbiguousModes` |
| 8 | 24 | 699,493 Hz |
| 9 | 25 | 801,073 Hz |
| 10 | 26 | 699,378 Hz |

Les événements 1–16 et 27 sont conservés dans la fenêtre brute, hors de cette séquence. Ils comprennent notamment des rejets `Noise` et des estimations à 171,3, 718,0 et 612,8 Hz. Leur origine physique n’est pas annotée. **Le nombre d’impacts réellement manqués ou de doubles détections ne peut pas être établi indépendamment**, faute de tops synchronisés ou d’observation audio/vidéo. Une séquence de dix détections compatible avec les dix coups déclarés est observée ; cela ne prouve pas à elle seule zéro impact manqué.

## Série complémentaire demandée après publication

L’utilisateur a demandé une nouvelle série de dix petits coups sur une seule vis pour compléter le rapport. Même APK, même support de guidage **vis 1/8**, nouvelle session de mesure (`session=2`, `generation=14`). La capture a été armée vide, en attente, « Suivant » désactivé. Consignes : petits coups à la baguette sur une vis physique fixe, point à 2,5 cm du cercle, téléphone et étouffement fixes, cinq secondes de silence avant/après, cadence demandée de deux secondes. L’utilisateur répond « fait », puis précise **« Un coup supplémentaire est possible »**. Le nombre physique exact n’est donc pas établi.

| Périmètre | Observations |
|---|---|
| Fenêtre complète entre marqueurs de début/fin | **98,449 s ; 19 détections, 10 estimations, 9 rejets** : 4 `AmbiguousModes`, 5 `Noise`. Deux événements `Confirmed`, aucun `Next`. Cette fenêtre inclut l’attente et la réponse de l’utilisateur. |
| Groupe régulier de détections 1–11 | **11 détections, 9 estimations, 2 rejets `AmbiguousModes`**. Durée première→dernière détection : **32,761 s** ; intervalles de **3,081 à 3,401 s**, moyenne **3,276 s**, plutôt que les deux secondes demandées. Aucun taux de détection sur « dix coups exacts » n’est calculé. |
| Première confirmation | À la **9e détection**, médiane **490,7105 Hz**, après trois estimations compatibles aux ID **6, 8 et 9**. Le rejet de l’ID 7 ne compte pas. Le nombre de coups physiques avant stabilité reste incertain ; le rang 9 désigne une détection. |
| Affinage sur la même vis | À l’ID **10**, nouvelle confirmation **490,7133 Hz**, série de quatre estimations. Même capture et vis ; aucun avancement. |
| Après le groupe régulier, toujours dans la fenêtre complète | **8 détections supplémentaires**, dont 7 rejets (2 `AmbiguousModes`, 5 `Noise`) et une estimation **384,539 Hz** à l’ID 19. Origine non annotée : ne pas les attribuer aux coups physiques de la série ni à une variation d’intensité. |
| État relu en fin de fenêtre | **384,5 Hz provisoire**, série à 1 ; **« Dernière mesure stable · 490,7 Hz »** conservée en dessous ; **« Suivant » désactivé**, toujours vis 1/8. Aucun nouveau `Confirmed` après l’ID 19. Cet état montre le maintien de l’ancienne confirmation lors d’une contradiction ; l’origine physique de l’événement contradictoire n’est pas établie. |

Détail du groupe régulier, en conservant les **onze** détections puisque le coup supplémentaire est possible :

| ID détecté | Résultat | Confirmation |
|---:|---|---|
| 1 | 800,329 Hz | — |
| 2 | 490,758 Hz | — |
| 3 | 800,324 Hz | — |
| 4 | 800,349 Hz | — |
| 5 | 698,966 Hz | — |
| 6 | 490,711 Hz | — |
| 7 | Rejet `AmbiguousModes` | La série reste à 1 |
| 8 | 490,656 Hz | — |
| 9 | 490,713 Hz | 490,7105 Hz ; série à 3 |
| 10 | 490,758 Hz | 490,7133 Hz ; série à 4 |
| 11 | Rejet `AmbiguousModes` | La série reste à 4 |

La disponibilité de « Suivant » juste après la confirmation n’a pas été photographiée pendant les frappes ; sa désactivation après la contradiction est présente dans l’export final. Aucun appui sur « Suivant » n’a été effectué. La justesse du mode confirmé vers 491 Hz n’est pas établie par un comparateur : cette confirmation prouve une cohérence entre quelques lectures, pas l’identification certaine de la résonance pertinente pour la vis.

### Retour utilisateur sur l’intensité

Après cette série, l’utilisateur rapporte : « j’ai l’impression que si je varie un peu d’intensité sur ma frappe, on a de grosse variations de fréquence ». Cette **sensibilité perçue à l’intensité est un point préoccupant pour la fiabilité de la mesure**, à conserver comme priorité de diagnostic. Les changements importants de lecture sont bien présents dans les métadonnées ; leur dépendance causale à l’intensité seule n’est pas démontrée. La force n’a pas été mesurée ou associée individuellement aux détections, et aucun PCM n’a été conservé.

Hypothèse à examiner : le mode retenu pourrait changer selon la frappe. Le rapport ne conclut ni que la fréquence pertinente de la peau change réellement dans les mêmes proportions, ni que l’intensité en est la seule cause. Pour comparer les vis, le choix de fréquence doit être suffisamment reproductible dans le geste prescrit. Ce point reste à résoudre ; il ne doit pas être masqué par un rabattement d’octave ou un choix du candidat proche de la cible.

## Ce que l’essai valide et ce qu’il laisse ouvert

- La mesure reste sur la **même vis** sans appui. Aucun événement `Next` pendant les frappes. Les rejets et remises à zéro sont distincts dans la trace. Les écrans de fin des séries montrent une lecture provisoire et le bouton désactivé.
- La répétabilité de la fréquence près d’une vis **n’est pas validée** dans ces conditions : les lectures acceptées alternent entre plusieurs groupes très éloignés. Les valeurs 491/699/801 ne constituent pas un simple saut systématique d’octave. La justesse de chacune n’est pas établie faute de comparateur.
- La confirmation préliminaire vers 598 Hz prouve qu’une série peut se stabiliser ; elle ne prouve pas que le mode choisi est le bon. Une alternance entre résonances reste une hypothèse à examiner. La trace ne contient pas les candidats spectraux ni leurs courbes de décroissance, et ne suffit pas à attribuer une cause.
- Les deux **premières** séries demandées n’ont pas atteint « Mesure stable ». Le complément obtient une confirmation à la 9e détection puis un affinage à la 10e ; le nombre physique exact reste incertain. Le maintien d’une ancienne confirmation sous une nouvelle lecture contradictoire est relu à l’écran, mais l’origine de cette contradiction n’est pas annotée. Le remplacement par une autre fréquence reconfirmée et l’appui sur « Suivant » après stabilité ne sont pas validés physiquement ; leurs tests injectés restent distincts.
- Aucun vrai tom disponible, confirmé par l’utilisateur. Le geste guidé sur tom et sa seconde peau ne sont pas testés physiquement. Aucun guidage produit de caisse claire n’est ajouté.
- Session abandonnée après le relevé, retour à la fiche, micro arrêté (`appops` sans capture en cours), snapshot des kits identique avant/après. Aucune référence ni mesure globale enregistrée.

**Suite technique proposée pour une prochaine demande :** examiner en priorité la sensibilité du choix de mode à l’intensité de frappe signalée par l’utilisateur, les conditions qui laissent accepter successivement plusieurs modes éloignés, puis l’attribution de détections hors des coups attendus. Enrichir les métadonnées des candidats et de leur persistance pourrait aider sans conserver de PCM. Ne pas corriger les Hz par rabattement d’octave ni par rapprochement de la cible ; ne pas conclure à un étalonnage à partir de cet essai. Aucune nouvelle correction ni nouvelle compilation n’a été faite pendant cette session physique.

Preuves du complément : [préparation et ressenti utilisateur](preuves/essai-physique-2026-11-25/serie-complementaire-10/preparation.json), [trace de la fenêtre complète](preuves/essai-physique-2026-11-25/serie-complementaire-10/serie.log), [résumé complet](preuves/essai-physique-2026-11-25/serie-complementaire-10/resultats.json), [groupe régulier](preuves/essai-physique-2026-11-25/serie-complementaire-10/groupe-regulier.log), [résumé du groupe](preuves/essai-physique-2026-11-25/serie-complementaire-10/resultats-groupe-regulier.json), [état final](preuves/essai-physique-2026-11-25/serie-complementaire-10/apres-serie.xml), [clôture](preuves/essai-physique-2026-11-25/serie-complementaire-10/cloture.log). Les événements postérieurs au marqueur de fin de série, présents dans le journal continu, restent hors des comptes ci-dessus. La session complémentaire a été abandonnée, micro arrêté et kits inchangés. Aucun nouveau build ni changement audio. Les comptes, fréquences, cadence, états d’écran, liens de preuve et empreintes ont été recalculés : [vérification du complément](preuves/essai-physique-2026-11-25/serie-complementaire-10/verification-rapport.log).

## Preuves et vérifications

Dossier : [preuves/essai-physique-2026-11-25](preuves/essai-physique-2026-11-25).

- [Préparation et empreintes](preuves/essai-physique-2026-11-25/preparation.json), [trace complète de métadonnées](preuves/essai-physique-2026-11-25/microphone-session.log).
- [Préliminaire](preuves/essai-physique-2026-11-25/preliminaire-summary.json), [fenêtre des dix coups](preuves/essai-physique-2026-11-25/dix-coups.log), [résumé brut](preuves/essai-physique-2026-11-25/dix-coups-summary.json), [séquence par cadence](preuves/essai-physique-2026-11-25/dix-coups-sequence-cadence.json).
- [Coups plus francs](preuves/essai-physique-2026-11-25/coups-francs.log), [résumé](preuves/essai-physique-2026-11-25/coups-francs-summary.json), [écran final](preuves/essai-physique-2026-11-25/apres-coups-francs.png), [état accessible final](preuves/essai-physique-2026-11-25/apres-coups-francs.xml), [clôture et conservation des kits](preuves/essai-physique-2026-11-25/cloture.log).

Commandes réalisées : installation ciblée `adb -s PIXEL install -r` du seul APK final ; vérification SHA-256 du `base.apk` installé ; lecture du snapshot par `run-as` avant/après ; `logcat -v epoch TomTuneTrace:I '*:S'` et marqueurs de phases ; export `uiautomator dump` ; résumés via `tools/summarize_lug_trace.py` ; abandon dans l’app ; `cmd appops get … RECORD_AUDIO`. L’identifiant matériel du Pixel n’est pas publié. Les métadonnées sont vérifiées par recalcul des comptes, fréquences et séquences ; aucun nouveau build ni test synthétique n’est requis pour ce compte rendu documentaire.
