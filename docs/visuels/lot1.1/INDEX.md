# Visuels pilotes Android

| Fichier | Clé | Dimensions |
|---|---|---|
| [kit-r3-f2-alpha.png](kit-r3-f2-alpha.png) | `r3-f2` | 1536 × 1024 |
| [rack-12-6-alpha.png](rack-12-6-alpha.png) | Rack tom 12″ à six vis | 1254 × 1254 |

Les PNG comportent un canal alpha et une ombre incorporée. Les utiliser sur le fond studio de la charte. Embarquer les ressources dans l’APK. Les ombres font partie du cadrage. Pour la fiche fût, le mot TomTune sert de repère sur la peau. Ce lot n’utilise pas l’image comme carte de coordonnées des vis.

## Zones du kit

[kit-r3-f2-zones.json](kit-r3-f2-zones.json) est calibré sur ce PNG uniquement. Les sommets sont exprimés entre 0 et 1 avec l’origine en haut à gauche. `hitOrder` est l’ordre de résolution, avec le premier polygone qui contient le toucher retenu. `rack1` à `rack3` désignent les racks de gauche à droite. `floor1` est près de la grosse caisse et `floor2` au premier plan. Les clés sont des emplacements du visuel, à associer aux ID des fûts du kit actif comme dans le lot 1.

Calculer les coordonnées à partir du **rectangle dessiné par l’image** dans la boîte, y compris ses décalages avec l’ajustement proportionnel. Par exemple, un toucher à `x` devient `(x - imageLeft) / imageWidth`. Ignorer les touchers hors du rectangle et des polygones. Appliquer la même transformation aux éléments accessibles et à tout repère de débogage. Garder ensemble la clé, le bitmap, le manifest et l’association aux ID lors des transitions.

Vérifier les zones sur téléphone pendant la validation de 1.1A. Les points servent à sélectionner les fûts dans la vue kit. Les zones des anciens PNG restent associées à leurs propres ressources.

## Activation 1.1B au lot 2–3 Android

Les **26 déclinaisons poussées avec la préparation du lot** sont présentes : sept kits et dix-neuf fûts, en complément des deux pilotes. Tous les PNG fournis sont conservés tels quels. Dimensions identiques aux sources ; contrôle des 26 nouvelles déclinaisons : aucun pixel RGB modifié, seul l’alpha change. Les deux pilotes validés restent inchangés. Relevé : `docs/lot2-3/preuves/assets-rgb.log`.

Les sept nouveaux `kit-rX-fY-zones.json` reprennent le format du pilote, avec association explicite au PNG exact, dimensions, empreinte et `verified: true`. Peau et coque ont été inspectées par superposition sur chaque PNG ; ordre : floor2, floor1, caisse claire, racks de gauche à droite, grosse caisse. Tests des points peau/coque et d’occlusion dans `tools/check_lot23_assets.py`. Le chargeur publie toujours ensemble le kit immuable, l’image et les zones associées aux ID. Le schéma reste utilisé pour zéro floor et en cas d’échec de chargement.

Toutes les fiches couvertes par `catalogue-focus.json` affichent maintenant leur variante alpha exacte. Le catalogue historique est lu avec ses points, mais leur présence ne vaut pas vérification des têtes réelles ni validation de la face opposée.

**Guidage activé en photo :** uniquement frappe du rack 12″ à six vis, avec son propre [manifest](rack-12-6-lugs.json), vérifié sur `rack-12-6-alpha.png`. Les six points ont été contrôlés sur ce PNG, index 0 à droite du logo, puis sens horaire. Image carrée ajustée proportionnellement ; flèche, toucher et accessibilité utilisent le même point. La résonance et les autres combinaisons utilisent un schéma tactile de la peau active avec le nombre réel de vis (3 à 12). Les autres photos restent disponibles pour la consultation, sans activer des ancrages de guidage non vérifiés.

Le fond continu de cette livraison est le beige rosé uni `#E3D1CA` défini par le brief 2–3. La précision et le confort des zones sur téléphone, ainsi que TalkBack, restent à confirmer physiquement.

### Pilote photo floor 16″ / huit vis

À la demande du propriétaire, le guidage photo ajoute `floor-16-8-lugs.json`, vérifié sur les huit têtes de `floor-16-8-alpha.png`. Le composant partagé place les numéros au-dessus du fût pour les vis supérieures et sur la peau au-dessus des vis inférieures. La tête reste visible ; flèche, numéro et accessibilité partagent l’index. Le chargement des manifests est générique par variante, avec repli pour les cartes ou faces absentes. La validation visuelle sur le téléphone commence par ce floor 16″ ; la déclinaison des autres photos suit son acceptation. Voir le [suivi de révision](../../lot2-3/RESULTATS.md#retour-utilisateur-et-pilote-photo--révision-du-24-novembre).
