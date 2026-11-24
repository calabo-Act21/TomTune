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
