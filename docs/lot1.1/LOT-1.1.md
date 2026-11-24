# Lot 1.1 — Accueil et fiche fût sur Android

## Départ et mission

Repartir de `main` après intégration du lot 1 ([PR #2](https://github.com/calabo-Act21/TomTune/pull/2)). Garder ses données locales et ses parcours de création, d’édition et de sélection. Ouvrir [les exigences V11 de la spec](../../SPEC.md), [le parcours annoté](../maquettes/PARCOURS.md) et [la charte](CHARTE.md).

Ce lot comporte deux validations successives. **1.1A**, immédiatement réalisable avec les deux visuels pilotes fournis, livre l’interface et valide le rendu sur Android. **1.1B** déploie le traitement visuel sur les variantes restantes après validation des pilotes et fourniture des images correspondantes. Le premier prompt s’arrête à 1.1A. L’accordage, le microphone, le contrôle du kit et le portage iOS gardent leur place dans les lots ultérieurs.

## 1.1A — Accueil

1. Appliquer le fond continu dès le conteneur de l’écran. Étendre la couleur aux zones système en mode bord à bord et garder les commandes dans la zone sûre.
2. Placer le kit juste sous le sélecteur de kit actif et le menu. Dessiner l’instrument à sa taille maximale dans le rectangle disponible en conservant son rapport largeur/hauteur. Garder une marge horizontale de 8 dp, l’intervalle défini dans la charte et de la place pour les commandes. Sur un écran étroit, donner priorité à la largeur et à la présence de tous les fûts. Éviter la grande boîte verticale centrée du lot 1. Un fond continu peut occuper l’espace restant. Garder néanmoins le visuel et le texte d’aide regroupés en haut.
3. Utiliser le pilote détouré pour `r3-f2`, avec l’ombre de contact intégrée et les zones décrites dans [l’index des pilotes](../visuels/lot1.1/INDEX.md). Les sept autres configurations à un ou deux floor toms continuent d’utiliser les images existantes pendant le pilote. Le schéma sans floor tom conserve ses fûts visibles et interactifs.
4. Pour le pilote, la zone d’un fût couvre **peau et coque visibles**. Prioriser la portion au premier plan lorsqu’il y a recouvrement. L’accastillage périphérique, pieds et pédale peuvent rester hors cible. Pour les autres compositions, garder les zones existantes tant que les nouveaux visuels ne sont pas fournis. Garder un seul élément accessible par fût avec libellé et action associés au même ID. Les zones suivent le rectangle réellement dessiné, décalage compris, sur toutes les tailles d’écran.
5. Corriger le décalage relevé en revue du lot 1. Le rendu interactif doit être identifié par la clé visuelle et le kit actif. Quand l’image n’est pas prête, afficher un état de chargement sans zone interactive. Publier le bon bitmap et ses zones ensemble. Écarter toute réponse tardive pour une sélection devenue obsolète. Si une ressource échoue, afficher le schéma du bon kit avec ses propres zones. Le bouton de retour et le sélecteur restent disponibles.

## 1.1A — Fiche fût

- Afficher un bandeau compact avec le retour « Mon kit » et le titre déroulant, par exemple **Rack tom 12″ ▾**. La liste contient les fûts du kit actif, un par ID, avec le fût courant marqué. Pour deux fûts du même type et diamètre, ajouter un ordinal dans la liste ou le libellé accessible. Conserver l’ordre visuel du lot 1.
- Toucher un choix remplace la fiche sur place. Il n’y a pas d’empilement de fiches. Le retour Android ou « Mon kit » ramène en un geste au même kit. Le changement de fût ne modifie ni sa configuration, ni le kit actif. Sur les futures fiches d’accordage, le changement de fût devra fermer la capture précédente et demander confirmation s’il abandonne une passe en cours.
- Donner à l’instrument la largeur utile maximale, conserver sa silhouette et son ombre entières. Le pilote concerne uniquement le rack 12″ à six vis. Les autres visuels existants restent disponibles avec le même cadrage d’écran et la même hiérarchie. Pour une combinaison hors catalogue ou une image indisponible, garder une fiche de données.
- Supprimer la carte qui répète le titre et le diamètre. Afficher le nombre de vis sur une ligne secondaire pour un tom, et la mention illustrative de la caisse claire lorsque nécessaire. Sur une fiche sans image, conserver les informations utiles. Prévoir l’espace pour les futures commandes d’accordage sans ajouter de boutons inactifs dans la fiche.

## Critères d’acceptation de 1.1A

| ID | Geste sur Android debug | Résultat observable |
|---|---|---|
| P1 | Ouvrir le kit 3+2 sur un écran étroit puis sur un écran plus large | Pas de bord rectangulaire autour du détourage, fond continu, pédale et pieds entiers, image rapprochée du sélecteur |
| P2 | Ouvrir le rack 12″ à six vis | Coque et ombre bien lisibles, aucune coupe des tirants, titre visible, diamètre indiqué une seule fois, nombre de vis lisible |
| P3 | Toucher la peau et les portions visibles de coque de chaque fût du kit pilote | Les deux touchers ouvrent la même fiche. Dans le chevauchement des floor toms, le fût visible au premier plan répond |
| P4 | Changer plusieurs fois de kit, avec un décodage volontairement retardé ou des changements rapprochés | Aucune zone active sur une ancienne image. Dès que la photo s’affiche, le toucher ouvre le fût du kit affiché. Valider aussi 0 floor |
| P5 | Utiliser le titre pour passer à trois fûts, puis Retour | La sélection change sur place et le bouton revient au même kit en un geste |
| P6 | Essayer un fût hors catalogue et relancer avec texte agrandi ou TalkBack | La fiche de données reste lisible, les fûts et le sélecteur sont accessibles, les contrôles restent dans les zones sûres |
| P7 | Relancer l’APK en mode avion avec un kit enregistré | L’accueil et les images embarquées sont disponibles. Les kits et la sélection sont préservés |

Comparer des captures avant/après sur le même téléphone. Tester un deuxième rapport d’écran via émulateur si disponible. Pour le pilote, vérifier sur un fond clair puis sur une teinte volontairement différente que le contour ne présente ni liseré opaque ni ombre coupée. Retenir le fond de la charte pour la revue utilisateur. Noter la validation physique distinctement des vérifications de code et de l’émulateur.

## 1.1B — Déploiement après validation

Détourer les images restantes sur le même fond d’essai en protégeant les reflets, les tirants et les ombres de contact. Pour chaque variante kit, fournir une carte des surfaces visibles, peau et coque, avec la priorité d’occlusion. Vérifier les zones et les fûts sur les huit configurations. Pour chaque image individuelle, conserver le nombre de vis et la quincaillerie validés et recalibrer les ancrages si le détourage change le repère. Retirer les chemins de transition du pilote après intégration des assets correspondants. Le mode zéro floor reste schématique.

## Livraison légère

Un agent par défaut. Conserver les tests existants. Ajouter au besoin un test ciblé sur le cas image/zone désynchronisée. Faire `./gradlew :composeApp:assembleDebug --console=plain` en fin d’itération et relancer seulement si nécessaire pour corriger un échec. Garder l’audio et les cibles iOS hors du diff fonctionnel. Ne pas déclencher manuellement la CI, publier ni ouvrir de PR avant le feu vert du propriétaire. Donner le chemin exact de l’APK, la commande d’installation et le résultat pour P1 à P7. La validation visuelle finale est faite par le propriétaire sur Android.
