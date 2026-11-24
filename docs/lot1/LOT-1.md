# Lot 1 — Configurer et retrouver ses kits sur Android

## Point de départ

Continuer le projet Kotlin Multiplatform et Compose du lot 0. Utiliser un commit qui contient la PR #1, ses modules Gradle et les fichiers de ce dossier. Si la PR #1 est encore ouverte, baser le travail sur une branche qui contient son code et la passation, ou intégrer la PR après décision de son propriétaire. Vérifier la présence de `gradlew`, `composeApp` et `shared` avant de coder. Le lot 0 et ses tests restent un point de comparaison.

## Périmètre

Livrer sur Android le premier lancement, la création, l’aperçu, la sauvegarde, la sélection et la modification de kits nommés, puis la vue interactive et une fiche de consultation par fût. Garder les modèles et règles partageables et les chaînes dans les quatre catalogues FR/EN/ES/DE. Les essais iPhone et l’implémentation de la persistance iOS sont différés. Conserver les cibles iOS existantes et leurs points d’entrée.

Le microphone, le réglage par vis, les notes de référence éditables et le contrôle du kit sont traités dans les lots suivants. L’accueil indique les deux entrées futures du menu en les laissant désactivées avec un libellé localisé « À venir ». La fiche affiche le fût, son type, son diamètre et, pour un tom, le nombre de vis. Le bouton de retour ramène au kit actif. Aucune demande d’autorisation micro dans ce parcours.

## Parcours à reproduire

Utiliser [le filaire validé](../maquettes/TomTune-maquettes.html) et [le parcours annoté](../maquettes/PARCOURS.md). Les écarts intentionnels par rapport au HTML historique sont listés dans le parcours annoté.

1. **Installation vierge.** Écran « Définir mon kit ». Ouvrir la configuration avec un rack tom et un floor tom. La caisse claire et la grosse caisse sont toujours présentes.
2. **Configuration.** Proposer 0 à 3 rack toms et 0 à 2 floor toms. Afficher des valeurs sélectionnables sur ces plages. Chaque fût a un diamètre éditable en pouces. Chaque tom a un nombre de vis éditable. Les valeurs proposées sont des choix initiaux et restent modifiables.
3. **Aperçu et nom.** « Voir mon kit » ouvre un aperçu du brouillon avec un champ de nom et « Enregistrer sur cet appareil ». En revenant en arrière, retrouver le brouillon en cours. Valider via le bouton ou le clavier. Accepter 1 à 60 caractères après retrait des espaces de bord. Les doublons sont comparés sans distinction de casse et indiqués au niveau du champ. Au premier enregistrement, ouvrir l’accueil et sélectionner le kit. Si l’écriture échoue, conserver le brouillon et afficher une erreur avec possibilité de réessayer.
4. **Accueil.** Afficher le sélecteur du kit actif en haut, le menu ☰ à droite et la vue du kit comme contenu principal. Le menu contient « Contrôle de l’accordage », « Notes de référence », « Modifier le kit » et « Créer un kit ». Ouvrir les fiches en touchant les fûts. Exposer les mêmes cibles aux services d’accessibilité.
5. **Édition et changement de kit.** Créer ajoute un nouveau kit avec les valeurs initiales. Modifier part d’une copie du kit actif et met à jour son identifiant existant. Annuler laisse les données précédentes intactes. Changer de kit restaure immédiatement sa composition. Après arrêt complet puis relance hors ligne, retrouver la liste et le dernier kit activé.

| Champ | Choix en pouces | Valeur proposée |
|---|---|---|
| Rack toms | 8, 10, 12, 13, 14 | 1 tom : 12. 2 toms : 10 et 12. 3 toms : 10, 12 et 13 |
| Floor toms | 14, 15, 16, 18 | 1 tom : 16. 2 toms : 16 et 18 |
| Caisse claire | 12, 13, 14, 15 | 14 |
| Grosse caisse | 18, 20, 22, 24, 26 | 22 |

Proposer 5 vis pour un tom de 10″ ou moins, 6 de 11 à 14″ et 8 au-delà. Le contrôle permet 3 à 12 vis entières. Présenter d’abord les valeurs du catalogue correspondant au type et au diamètre, puis permettre une valeur personnalisée. Conserver la suggestion tant qu’elle existe au catalogue, ou utiliser la valeur proposée dans l’index. Le nombre de vis est une propriété du fût configuré. En changeant le diamètre, recalculer la suggestion seulement tant qu’aucune valeur manuelle n’a été choisie. Lorsque le nombre de toms augmente, préserver les fûts existants et ajouter les nouveaux avec les suggestions prévues, en préférant une taille encore disponible. Quand le nombre diminue, retirer les derniers fûts ajoutés du type concerné, sans modifier les identifiants conservés. Dans l’aperçu et l’accueil, ordonner les toms d’un même type par diamètre croissant, puis par ordre de création en cas d’égalité. Une modification d’ordre ne change pas les identifiants.

## Composition et assets

Utiliser la table [index des visuels](../visuels/INDEX.md). Choisir le PNG par **nombre de rack toms et de floor toms** et l’embarquer dans l’application pour une utilisation hors ligne. Les dimensions photographiques représentent des diamètres typiques. Les diamètres réellement saisis figurent dans la fiche et les libellés accessibles. Une zone transparente correspond à chaque fût visible, se déforme avec le rectangle effectivement occupé par l’image et ouvre l’ID du fût dans le même ordre que ses voisins. Préserver l’aspect de l’image entière. Garder une cible tactile distincte par fût et éviter que deux zones se disputent le toucher.

Les huit images couvrent les kits avec un ou deux floor toms. Pour zéro floor tom, garder le choix valide et afficher dans le lot 1 une vue schématique interactive des seuls fûts présents, avec les mêmes libellés et la même navigation. Le téléchargement d’images et la production de quatre images supplémentaires sont hors lot. Pour la fiche du fût, rechercher une image par type, diamètre et nombre de vis par peau dans le [catalogue des visuels](../visuels/INDEX.md). En l’absence de correspondance exacte, présenter le type, le diamètre et le nombre de vis dans une fiche de données. Pour la caisse claire de 14″ en consultation, utiliser la variante 8 vis comme illustration par défaut et traiter cette valeur comme illustrative tant que la configuration ne la renseigne pas. L’interaction par vis et la flèche viendront dans le lot d’accordage.

## Données

Un kit et ses fûts ont des identifiants stables indépendants du nom et des libellés. Stocker, sous un schéma versionné, les kits, leur ordre, les types, l’ordre des fûts, les diamètres, les vis et l’ID du kit actif. Inclure un contrat de dépôt partagé et une persistance locale Android app-privée derrière ce contrat. Réutiliser une persistance existante si elle convient. Pour cette première version et en l’absence d’existant, enregistrer un document JSON versionné écrit atomiquement hors du thread d’interface. Ajouter la liste et la sélection dans une même écriture atomique à la création. Charger un état valide après relance et rendre un échec lisible à l’utilisateur. Empêcher un double enregistrement sur deux pressions rapides.

## Validation Android à effectuer

| ID | Action sur le téléphone | Résultat observable |
|---|---|---|
| A1 | Ouvrir une installation vierge | « Définir mon kit » avec 1 rack et 1 floor par défaut |
| A2 | Choisir 3 racks et 2 floors, modifier un diamètre et des vis, ouvrir l’aperçu | Image 3+2, nom saisissable, données conservées après retour à la configuration |
| A3 | Essayer un nom vide, puis nommer le kit et enregistrer | Erreur en cas de nom vide, puis accueil et sélection du kit nommé |
| A4 | Toucher successivement caisse claire, grosse caisse, les 3 racks et les 2 floors | Chaque touche ouvre la fiche du bon fût. Le retour conserve le kit |
| A5 | Créer un kit 0 rack / 1 floor et tenter le même nom | Image 0+1, erreur de doublon, deux kits distincts après renommage |
| A6 | Changer de kit, éditer puis annuler. Refaire une édition et enregistrer | Le premier essai ne change rien. Le second modifie uniquement le kit actif |
| A7 | Activer le mode avion, forcer l’arrêt puis relancer | Les deux kits et le kit actif sont conservés |
| A8 | Configurer zéro floor et toucher les fûts visibles | Seuls les fûts configurés apparaissent, la fiche correcte s’ouvre |
| A9 | Comparer un rack 12″ à 6 puis à 8 vis, un floor et la caisse claire | La fiche montre la bonne famille de fûts. Le rack affiche le nombre exact de vis enregistré. Un choix hors catalogue affiche les données du fût |

## Boucle de livraison légère

- Un seul agent par défaut. Lire les fichiers d’entrée du lot et les points d’entrée concernés. Partager un compte rendu court avec les changements, les commandes, leurs résultats et les blocages éventuels.
- Garder les tests existants. Ajouter un test ciblé uniquement pour une régression concrète que les vérifications existantes ne couvrent pas. Lancer les tests audio seulement si ce code change. Au niveau de l’état partagé, tester une régression précise du stockage ou de l’association visuel/kit si elle ne peut être validée correctement sur téléphone.
- Faire un build Android debug en fin d’itération avec `./gradlew :composeApp:assembleDebug --console=plain`. En cas d’échec, lire l’erreur, corriger et relancer la vérification ciblée. Réutiliser l’APK pour les essais suivants tant que le code n’a pas changé. Indiquer le chemin de l’APK et la procédure d’installation.
- Ne pas déclencher manuellement les workflows CI ni lancer Xcode pour cette itération Android. Faire la validation complète à un jalon décidé. Livrer une branche locale avec un diff prêt pour le propriétaire, qui valide l’APK debug sur son téléphone avant la PR. Si le dépôt déclenche automatiquement sa matrice sur une PR, attendre le feu vert du propriétaire avant de l’ouvrir.
