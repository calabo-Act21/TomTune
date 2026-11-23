# TomTune — Spécification v0.3

Les parcours et la direction visuelle sont validés. Cette version fixe le périmètre de la V1 et retient Kotlin Multiplatform avec Compose pour le lot 0. Les paramètres d’analyse audio restent des hypothèses tant que les essais sur des batteries réelles ne les ont pas validés.

- [Maquette filaire cliquable](docs/maquettes/TomTune-maquettes.html) pour les interactions.
- [Direction visuelle validée](docs/maquettes/direction-visuelle.png) pour les volumes et les matériaux.
- [Mission du lot 0](docs/lot0/LOT-0.md) pour l’écran à construire et les essais.

## 1. Périmètre

Application mobile pour configurer une batterie, accorder ses toms, gérer ses références et contrôler l’ensemble du kit. La V1 propose l’accordage guidé des toms et la mesure globale de la caisse claire et de la grosse caisse. Les parcours visés fonctionnent hors connexion avec des kits enregistrés sur l’appareil.

Pour démarrer, je propose iOS et Android, une interface portrait et un traitement audio sur l’appareil. Le compte, la synchronisation et l’export sont hors périmètre de cette V1. L’app conserve les valeurs de mesure et la configuration. Les échantillons du microphone sont traités en mémoire puis libérés.

## 2. Parcours et critères d’acceptation

### KIT — Configurer et retrouver ses kits

- **KIT-01.** Au premier lancement, proposer « Définir mon kit ». La configuration commence avec un rack tom et un floor tom. Accepter une saisie entière libre pour chaque nombre, dont zéro et les valeurs au-delà de deux. Demander les diamètres. Préremplir le nombre de tirants par tom et permettre de le corriger. La caisse claire et la grosse caisse font partie du kit et ont un diamètre configurable.
- **KIT-02.** Afficher un aperçu. Pour enregistrer, demander un nom non vide après retrait des espaces de bord. Si un nom est déjà utilisé, afficher une erreur près du champ. Le bouton et la validation clavier doivent fonctionner. Un enregistrement réussi ouvre le kit créé et le sélectionne. Créer un second kit, redémarrer hors ligne puis retrouver les deux et le kit actif.
- **KIT-03.** Faire de la scène interactive le contenu principal. Le sélecteur de kits est en haut, le bouton ☰ ouvre « Contrôle de l’accordage », « Notes de référence », « Modifier le kit » et « Créer un kit ». Modifier le kit actif met à jour son propre enregistrement. Annuler l’édition ne change ni ce kit ni les autres.
- **KIT-04.** Garder des identifiants stables pour les fûts conservés pendant l’édition. Je propose de conserver la référence et la dernière mesure lors d’un renommage ou changement de disposition, de les effacer si le diamètre change et d’invalider les mesures près des vis si leur nombre change. Tester ces trois transitions.

### 3D — Repérer le bon fût et les bonnes vis

- **VIS-01.** Vue principale depuis le tabouret. Grosse caisse devant le batteur, rack toms au-dessus en arc, caisse claire au premier plan. Les deux premiers floor toms sont à droite de la grosse caisse, le troisième à gauche. Le tabouret apparaît sans texte. Tester les configurations avec zéro à trois floor toms. Au-delà, utiliser provisoirement un placement par rangées sans recouvrement des cibles tactiles. Garder le calcul de disposition isolé.
- **VIS-02.** Toucher un fût ouvre sa fiche. La scène a des volumes réels, avec bois, métaux, ombres, coque et cercle inférieur visibles. Sur la fiche d’un tom, prendre une vue presque verticale légèrement inclinée. La peau de frappe est affichée par défaut, puis la peau active pendant le guidage.
- **VIS-03.** Pour N vis, utiliser des angles régulièrement espacés et décalés d’un demi-intervalle autour du haut de la peau. Dans un repère écran où le haut vaut −π/2, l’angle de la vis i vaut −π/2 + 2π(i+0,5)/N, avec i de 0 à N−1. TomTune est centré entre les deux vis qui encadrent midi. Tester le nombre de vis, leur symétrie et la correspondance vis active/indice pour N = 5, 6, 8 et 10.
- **VIS-04.** Vérifier sur téléphone les intersections entre géométries, les occultations en perspective, le cadrage et la sélection du bon fût. Prévoir un accès textuel aux mêmes fûts pour l’accessibilité. Les scènes doivent rester fluides pendant la mesure micro.

### ACC — Accorder un tom

- **ACC-01.** Sur la fiche, afficher l’état d’accordage, la référence enregistrée si elle existe et la dernière mesure si disponible. Démarrer l’accordage avec la référence existante, ou une note suggérée selon le type et le diamètre du tom. Les suggestions doivent aussi respecter la progression grave/aigu du kit. Autoriser un changement de cible pour cette session.
- **ACC-02.** Guider d’abord la peau de frappe. Donner la fréquence cible près des vis en Hz, montrer le repère et faire ressortir la vis active. Demander de frapper doucement près de la vis et d’étouffer la peau opposée. Indiquer visuellement « Frappe captée », afficher la fréquence provisoire et signaler une série instable. Valider automatiquement une fréquence stable, avancer d’une seule vis et laisser visible la dernière mesure confirmée.
- **ACC-03.** Quand le tour est terminé, afficher pour chaque vis « Serrer », « Desserrer » ou « Laisser », avec symbole, couleur et libellé accessible. Un ordre de réglage peut être proposé. « Vérifier cette peau » lance un nouveau tour, autant de fois que nécessaire. « Passer à la peau de résonance » reprend le même cycle avec la cible de cette peau.
- **ACC-04.** Après les deux peaux, demander de jouer au centre avec les deux peaux libres. Afficher note, Hz et écart à la cible en cents. Si l’écart reste significatif, proposer un passage de correction et permettre une nouvelle vérification. Terminer revient à la fiche et actualise la dernière mesure globale stable. Une action explicite permet d’enregistrer la cible de session comme référence. Un abandon, une série instable ou une frappe isolée ne modifie pas une mesure enregistrée.

### REF — Gérer les notes de référence

- **REF-01.** Depuis le menu ou la fiche, consulter les références par fût. Choisir une note, accepter une suggestion ou mesurer le fût au centre avec les deux peaux libres. Une capture stabilisée reste dans le brouillon jusqu’à confirmation. Pour un fût sans référence, afficher un état « À définir ».
- **REF-02.** Conserver la fréquence de la capture pour les comparaisons, même si la note est affichée avec un arrondi à la note la plus proche. Une saisie manuelle fixe la fréquence de la note choisie. Les changements de langue et l’affichage en Hz ne modifient pas cette valeur.

### CTR — Contrôler l’accordage du kit

- **CTR-01.** Mesurer la caisse claire, tous les toms par diamètre croissant, puis la grosse caisse. Pour les diamètres égaux, garder l’ordre du kit. Montrer le fût courant, le progrès et le retour visuel de mesure. Avancer seulement après une série stable ou après « Passer ». Signaler les fûts sans référence et ceux passés sans mesure.
- **CTR-02.** Afficher dans le bilan les références et les mesures du contrôle sur le visuel du kit, avec un détail lisible par fût. Proposer « Accorder ce tom » pour un tom dont l’écart stable dépasse le seuil configuré. Après le guidage, revenir au même bilan et remplacer la mesure du tom concerné. Sa référence est mise à jour uniquement si l’utilisateur a choisi de l’enregistrer. Une mesure historique ne remplit pas une case passée lors du contrôle en cours.

## 3. Audio et accordage

### Capture, choix de fréquence et stabilité

Un tom peut produire plusieurs résonances, dont certaines ne sont pas des multiples entiers de la fréquence recherchée. Le pic le plus fort peut changer entre l’impact et la décroissance. TomTune recherche la fréquence pertinente pour le geste demandé, puis vérifie qu’elle est reproductible entre frappes. Une série de frappes cohérentes ne suffit pas si l’estimateur a choisi une mauvaise résonance.

| Mode de mesure | Geste | Fréquence recherchée et affichage |
|---|---|---|
| Près d’une vis | Frapper doucement près de la vis sur la peau active et étouffer la peau opposée | Résonance locale pertinente pour comparer les vis de cette peau. Afficher des Hz |
| Fût entier | Frapper au centre avec les deux peaux libres | Hauteur globale du fût. Afficher la note, les Hz et l’écart à la cible si elle existe |

1. **Ouvrir la capture.** Demander le micro à la première mesure et ouvrir une seule capture au premier plan. En cas de refus, d’interruption ou de changement de route audio, afficher la cause et offrir une reprise ou une sortie. Fermer la capture en quittant le parcours ou en passant en arrière-plan. Ignorer les événements tardifs d’une étape quittée.
2. **Détecter l’impact.** Utiliser le PCM mono et le taux d’échantillonnage fourni par le système. Afficher « Frappe captée » dès qu’un impact est détecté, même si sa hauteur est encore inconnue. Associer au plus une estimation à chaque frappe. Séparer les attaques rapprochées lorsqu’elles correspondent réellement à deux frappes et ignorer l’écho ou la décroissance qui ne représentent pas un nouvel impact.
3. **Estimer la hauteur d’une frappe.** Analyser plusieurs fenêtres de la décroissance après le transitoire initial, tant que le signal reste au-dessus du bruit. Comparer des candidats spectraux et, lorsque pertinent, des indices de périodicité. Évaluer la persistance de la fréquence, sa qualité et les modes concurrents. Détecter les erreurs d’octave plausibles. La bande de recherche dépend du mode de mesure et du fût. La cible peut aider à orienter la recherche, mais ni la proximité de la cible ni l’amplitude maximale d’un pic ne suffisent à valider une lecture.
4. **Afficher le résultat de la frappe.** Si une fréquence est suffisamment étayée, l’afficher comme provisoire. Si le signal est saturé, trop bruité ou comporte plusieurs résonances impossibles à départager avec assez de confiance, afficher une mesure non concluante et proposer de refrapper. Ne pas présenter un candidat incertain comme une note ni l’ajouter à une série de validation.
5. **Valider entre frappes.** Accumuler uniquement des estimations individuelles exploitables. Comparer leur dispersion en cents. Une incohérence redémarre la série. Une série très cohérente peut être validée plus vite qu’une série marginale. Agréger les fréquences stables avec une médiane, émettre une seule validation par étape et réinitialiser la série au changement de vis, de peau ou de fût. La validation ne modifie jamais une référence enregistrée sans action explicite.

Le lot 0 s’arrête à l’estimation d’une frappe. La stabilité entre frappes vient ensuite. Pour cette étape ultérieure, on pourra tester une série courte et une série plus longue, la stabilité déterminant la validation. Calibrer la confiance, les bandes et les tolérances en cents séparément pour la vis, le tom entier, la caisse claire et la grosse caisse. Les seuils de qualité du signal et ceux des conseils d’accordage doivent rester indépendants.

### Vérification de la sélection de fréquence

Les tests couvrent une frappe simple, une résonance secondaire plus forte, un piège d’octave avec des indices suffisants pour déterminer la bonne fréquence, et deux modes réellement ambigus. Une lecture doit rester fiable quand la fréquence réelle s’éloigne de la cible. Le silence, le bruit et la saturation ne doivent pas produire une fréquence acceptée. Confirmer le résultat près d’une vis sur un vrai tom avant de calibrer la validation entre frappes. Les essais du lot 0 et leurs données sont décrits dans [la mission du lot 0](docs/lot0/LOT-0.md).

### Cibles et conseils

La fréquence fondamentale cible, les fréquences cibles près des vis et la fréquence globale mesurée sont distinctes. Appliquer un seul profil au départ, avec la peau de résonance plus aiguë. Les cibles par peau sont des estimations et doivent être revues à partir de la mesure du fût entier. Pour le prototype uniquement, comparer un départ vers 1,50 × la fondamentale côté frappe et 1,85 × côté résonance avec les mesures réelles. Isoler ces coefficients dans un profil versionné. La cible globale choisie reste l’objectif même si les coefficients sont corrigés.

Pour une mesure f et une cible t, l’écart vaut 1200 × log₂(f/t). Appliquer des seuils distincts pour les conseils près des vis et pour le contrôle global. Au-delà de la tolérance locale, une mesure basse donne « Serrer », une mesure haute « Desserrer ». Si les vis sont proches de leurs cibles mais que la fondamentale reste éloignée, prendre une nouvelle mesure globale puis proposer une correction prudente du profil ou un nouveau tour. Éviter de présenter un réglage automatique comme garanti.

## 4. Choix technique

Kotlin Multiplatform et Compose Multiplatform pour le lot 0. Réévaluer ce choix si les résultats sur appareils révèlent un blocage réel. Partager l’interface Compose et la logique de mesure Kotlin. Tester Filament sous Android et RealityKit sous iOS comme intégrations de départ pour la scène. La capture PCM utilise les API de chaque plateforme derrière un contrat commun. Les lots suivants détailleront les choix de persistance. Préparer les ressources de texte en FR, EN, ES et DE dès le premier écran.

Stocker une fréquence en Hz et une hauteur canonique indépendante de la langue. L’affichage proposé utilise Do/Ré/Mi en FR et ES, C/D/E en EN, H pour le si naturel et B pour le si bémol en DE. La notation des altérations et des octaves reste à relire avec des musiciens des langues cibles. Utiliser le format numérique local et conserver les unités Hz et cents.

## 5. Données et séparation des responsabilités

| Donnée | Contenu minimal |
|---|---|
| Kit | Identifiant stable, nom, configuration ordonnée, révision. Identifiant du kit actif dans les préférences |
| Fût | Identifiants du kit et du fût, type, ordre, diamètre, nombre de vis si applicable |
| Référence du fût | Hz cible, provenance manuelle/suggérée/capturée et date |
| Mesure globale | Hz stable, date, identifiant du fût et provenance du parcours |
| Session de guidage | Kit, fût, cible figée, profil, peau, index de vis, résultats stables du tour. État temporaire |
| Contrôle courant | Ordre figé, état par fût et mesure associée au contrôle. État temporaire jusqu’au bilan |

Le domaine Kotlin ne dépend pas du microphone, de la 3D ni de la persistance. Injecter une source audio simulée pour les tests et la démonstration. Les coordonnées de la scène dépendent du kit, le résultat d’une mesure dépend du contexte de capture, et l’écriture dépend de l’action métier. Conserver la mesure existante si une nouvelle action échoue. Rendre l’avancement idempotent lorsqu’un événement est reçu deux fois.

## 6. Lots à confier à Codex

| Lot | Travail | Condition de sortie |
|---|---|---|
| **0. Faisabilité** | Projet Kotlin Multiplatform et Compose. Un tom 3D tactile, capture PCM et fréquence visible. Voir la mission du lot 0 | Trace des essais sur iPhone et Android. Mesures de latence indicative et fluidité, échecs recensés et décision sur la stack |
| **1. Kits** | Domaine, migrations, configuration, aperçu, sauvegarde, sélection et édition. Vue 3D fonctionnelle, sans attente de finition | KIT-01 à KIT-04 et placements reproductibles de VIS-01 |
| **2. Mesure** | Adaptateur micro, détection de frappe, estimateur, stabilité, feedback et source simulée | Tests d’entrée silence, bruit, saturation, différence d’octave, signaux amortis, frappes rapprochées et interruptions. Validation unique et nombre variable de frappes |
| **3. Guidage** | Géométrie des vis, fiche, cible de session, tours des deux peaux et vérification globale | VIS-02 à VIS-04 et ACC-01 à ACC-04 sur une session simulée complète, puis essai réel sur au moins un tom |
| **4. Références et contrôle** | Référence au choix, suggérée ou capturée. Contrôle, bilan et reprise de l’accordage | REF-01, REF-02, CTR-01 et CTR-02. Contrôle de kit complet au micro et reprise d’un tom |
| **5. Finition** | Matériaux et éclairage de référence, calibration, relecture des langues, tests sur différents kits et téléphones | Matrice appareils/instruments, contrôles graphiques sur téléphone et compte rendu d’étalonnage |

Au lot 0, documenter les commandes Gradle et Xcode de build et de test qui s’appliquent au projet. Les commandes de vérification sont exécutées à chaque lot selon le périmètre. Pour les tests micro réels, noter l’appareil, le type et le diamètre du fût, le placement du téléphone, le contexte acoustique et l’instrument ou la méthode de comparaison. Sur des enregistrements annotés et des frappes au micro, relever l’erreur en cents, le délai et le nombre de frappes avant validation, les fausses confirmations, les octaves erronées et les échecs de mesure. Consigner les mesures avant de figer les seuils V1.

## 7. Organisation des sous-agents

| Sous-agent | Périmètre |
|---|---|
| Audio | Acquisition, estimation, stabilité, cas limites et mesures sur appareil |
| 3D | Modèles paramétriques, caméra, placement, rendu et sélection tactile |
| Produit et données | Configuration, navigation, stockage, références et localisation |
| Validation | Scénarios d’acceptation, données de test et vérification d’intégration |

L’agent principal fixe les interfaces partagées et l’ordre des lots. Déléguer en parallèle les investigations indépendantes. Si plusieurs agents écrivent du code, leur donner des fichiers distincts ou des branches séparées. Chaque agent rend les fichiers changés, les vérifications exécutées et les points non testés. L’agent principal intègre et vérifie les parcours complets.

## Sources techniques

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform/kmp-overview.html)
- [Compose et UIKit](https://kotlinlang.org/docs/multiplatform/compose-uikit-integration.html)
- [Filament](https://github.com/google/filament)
- [RealityKit](https://developer.apple.com/documentation/realitykit)
- [Tune-Bot — guide d’accordage](https://tune-bot.com/tuning-guide/)
