# TomTune — Parcours produit et maquette filaire

Ouvrir [TomTune-maquettes.html](TomTune-maquettes.html) dans un navigateur. Cette version validée illustre la navigation et les états. Les règles ci-dessous font foi pour les décisions postérieures sur les visuels et les nombres de fûts. Les valeurs audio et le bouton de simulation illustrent le parcours.

| Parcours | Écrans et transitions |
|---|---|
| Définir et retrouver un kit | Premier lancement → « Définir mon kit » → composition et diamètres → aperçu et nom → enregistrement → accueil. Le menu permet de créer ou modifier un kit. Le sélecteur du haut change de kit |
| Accorder un tom | Accueil → toucher le tom → fiche → choisir la cible globale → peau de frappe → chaque vis et mesure → répartition des corrections → nouvelle passe si nécessaire → peau de résonance → vérification du fût entier → fiche |
| Définir les références | Menu → « Notes de référence » → choisir le fût → choisir une note, prendre une suggestion ou capter la note centrale du fût → confirmer → liste avec la référence |
| Contrôler le kit | Menu → « Contrôle de l’accordage » → caisse claire → toms par diamètre croissant → grosse caisse → bilan par fût → accordage d’un tom si nécessaire → retour au même bilan et mise à jour de sa mesure |

## Évolutions après validation du filaire

- Le kit a 0 à 3 rack toms et 0 à 2 floor toms. Au départ 1 + 1. Les visuels réalistes fournis couvrent les huit variantes à 1 et 2 floor toms. À zéro, afficher une disposition interactive schématique dans le lot 1.
- Le point de vue du batteur est conservé, le tabouret est hors champ. Les rack toms sont de gauche à droite par diamètre, les deux floor toms à droite avec le plus petit près de la grosse caisse. Les fûts sont en bois bordeaux laqué à bords sombres, avec cercles et coquilles chromés.
- À l’accordage, utiliser des images de fûts avec l’accastillage complet. La flèche verticale est dessinée au-dessus de la vis active par l’app, au même point que la zone tactile. Le logo est entre les deux vis du haut. La vue du fût domine l’écran et les vis se touchent sur le cercle. Aucune rangée de boutons par vis. Un lecteur d’écran peut sélectionner chaque vis.
- Sur frappe près d’une vis, afficher les Hz et l’écart à la cible si disponible. Pour le fût joué au centre, afficher note, Hz et écart. Distinguer frappe captée, mesure provisoire, mesure non concluante et confirmation. Accepter un nombre variable de frappes selon leur concordance.
- Au retour d’un accordage, actualiser la mesure du fût. Sa référence change sur confirmation explicite. Lire [SPEC.md](../../SPEC.md) pour les exigences détaillées et [LOT-1.md](../lot1/LOT-1.md) pour le périmètre Android de l’itération.

## Évolutions du lot 1.1

- Sur l’accueil, le visuel du kit se place sous le sélecteur et s’ouvre au toucher sur la peau ou la coque. La sélection suit le fût visible au premier plan en cas de recouvrement.
- Sur la fiche, le titre devient un sélecteur affichant les fûts du kit actif. Toucher un fût de la liste remplace la fiche sur place. « Mon kit » ou le retour Android ramène en un geste au même kit.
- La charte et les règles d’affichage sont précisées dans [le brief du lot 1.1](../lot1.1/LOT-1.1.md). Le filaire historique reste la référence de l’enchaînement général.
