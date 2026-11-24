# Visuels TomTune

## Images du kit

Mode portrait ou paysage selon la surface disponible. Garder toute l’image visible et calculer les zones tactiles en fonction de la taille et de la position du contenu effectivement dessiné.

| Rack toms | Floor toms | Image |
|---:|---:|---|
| 0 | 1 | [r0-f1.png](kit/r0-f1.png) |
| 0 | 2 | [r0-f2.png](kit/r0-f2.png) |
| 1 | 1 | [r1-f1.png](kit/r1-f1.png) |
| 1 | 2 | [r1-f2.png](kit/r1-f2.png) |
| 2 | 1 | [r2-f1.png](kit/r2-f1.png) |
| 2 | 2 | [r2-f2.png](kit/r2-f2.png) |
| 3 | 1 | [r3-f1.png](kit/r3-f1.png) |
| 3 | 2 | [r3-f2.png](kit/r3-f2.png) |

Le fichier [zones-kit.json](zones-kit.json) donne les zones indicatives dans le repère de chaque image. Un rectangle normalisé suit l’ordre `[left, top, right, bottom]` avec origine en haut à gauche. Transformer le rectangle avec le même facteur et le même décalage que l’image. Les clés `rack1…rack3` et `floor1…floor2` repèrent les positions visuelles dans l’ordre de diamètre. Traduire chaque clé vers l’identifiant stable du fût du kit affiché. Les zones sont des points de départ pour les essais tactiles sur Android. Utiliser la fiche pour afficher les diamètres exacts du kit.

Les kits à zéro floor tom utilisent une vue schématique interactive dans le lot 1. Les images ci-dessus sont des variantes de composition. Elles ne changent pas pour chaque diamètre configuré.

## Catalogue des vues d’un fût

Le nombre correspond aux **tirants d’une seule peau**, pas aux deux peaux additionnées. Cette table regroupe les combinaisons attestées par les catalogues de pièces et quelques configurations rares identifiées. Un cercle disponible indique une possibilité de montage, sans établir la fréquence de ce montage sur les kits. Certaines séries anciennes ou fabrications sur mesure peuvent différer.

| Type | Diamètre | Vis par peau couvertes | Suggestion initiale |
|---|---:|---|---:|
| Rack tom | 10″ | [4](toms/rack-10-4-vis.jpg), [5](toms/rack-10-5-vis.jpg), [6](toms/rack-10-6-vis.jpg) | 5 |
| Rack tom | 12″ | [4](toms/rack-12-4-vis.jpg), [5](toms/rack-12-5-vis.jpg), [6](toms/rack-12-6-vis.png), [8](toms/rack-12-8-vis.jpg) | 6 |
| Rack tom | 13″ | [5](toms/rack-13-5-vis.jpg), [6](toms/rack-13-6-vis.jpg), [8](toms/rack-13-8-vis.jpg) | 6 |
| Floor tom | 14″ | [6](toms/floor-14-6-vis.jpg), [8](toms/floor-14-8-vis.jpg), [10](toms/floor-14-10-vis.jpg) | 6 |
| Floor tom | 16″ | [6](toms/floor-16-6-vis.jpg), [8](toms/floor-16-8-vis.jpg), [10](toms/floor-16-10-vis.jpg) | 8 |
| Floor tom | 18″ | [8](toms/floor-18-8-vis.jpg), [10](toms/floor-18-10-vis.jpg) | 8 |
| Caisse claire | 14″ | [8](toms/snare-14-8-vis.jpg), [10](toms/snare-14-10-vis.jpg) | 8 pour le visuel de consultation |

Références de matériel :

- [Gibraltar · cercles acier 2,3 mm](https://gibraltarhardware.com/products/triple-flanged-steel-drum-hoop) et [cercles acier 3 mm](https://gibraltarhardware.com/products/3-0mm-triple-flanged-steel-drum-hoop?variant=41126948798620), pour les combinaisons de catalogue.
- [Gibraltar · cercles de caisse claire dans le catalogue](https://www.yumpu.com/en/document/view/10961123/gibraltar-hardware-catalog-pdf), pour les variantes de caisse claire 14″.
- Configurations moins courantes, documentées par les pièces correspondantes. [Rack 10″ à 4 tirants](https://reverb.com/item/96869484-mendini-10-tom-drum-hoop-4-lug-black-1-6mm-triple-flanged-style), [rack 12″ à 4 tirants](https://www.ebay.com/itm/188358704111), [rack 13″ à 5 tirants](https://reverb.com/item/59365017-13-drum-hoops-5-lug-tom-rim-pair-chrome-1013), [floor 16″ à 10 tirants](https://www.grelly.uk/itm/16-premier-pre-international-die-cast-hoop-10-lug-floor-tom-rim-free-p-p-157867300892).
- [Catalogue de dimensions des fûts](https://pearldrum.com/en/products/drum-sets/decade-maple/decade-maple), utilisé comme référence de proportions. Les visuels TomTune ne portent ni marque ni badge constructeur.

### Intégration

Le fichier [catalogue-focus.json](catalogue-focus.json) fournit un chemin, un type, un diamètre, une profondeur de représentation, un nombre de vis et les points d’ancrage pour chaque asset. Les points sont des centres de têtes de vis normalisés `[x,y]` dans le repère de l’image, avec origine en haut à gauche et x allant vers la droite. L’indice 0 est le premier tirant à droite du logo à midi. Les suivants tournent dans le sens horaire. Appliquer la même transformation que pour le rectangle effectivement dessiné. Dessiner la flèche de façon à ce que sa pointe indique le point d’ancrage, en gardant les coordonnées tactiles au même point. Les JPEG sont des rendus statiques calculés avec le nombre exact de tirants, en finition bordeaux avec le repère TomTune. Aucun moteur 3D n’est requis dans l’application pour les afficher.

- [Rack tom 12″ à six vis, visuel approuvé sans flèche](toms/rack-12-6-vis.png) et [référence avec flèche](toms/rack-12-6-vis-reference.png). Ne pas embarquer la version avec flèche dans le parcours d’accordage.
- Pour un nombre ou un diamètre hors catalogue, conserver la configuration et éviter de montrer une image avec un nombre de vis différent. Dans le lot 1, afficher les données du fût. Le guidage pourra utiliser un schéma avec le nombre exact de vis.
- Les visuels de caisse claire servent à sa fiche et à la mesure du fût entier. Le lot 1 ne crée pas d’écran d’accordage par vis pour la caisse claire.
