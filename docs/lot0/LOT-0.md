# Lot 0 — Faisabilité Kotlin

## Mission

Un écran Compose affiche un tom en 3D légèrement de biais depuis le dessus, avec une vis tactile et un repère entre les deux vis du haut. Il donne un retour « Frappe captée » et affiche la fréquence provisoire en Hz lorsqu’elle est exploitable. Le reste du produit est décrit dans [la spec](../../SPEC.md). La maquette filaire aide à retrouver les états et l’image fournit la direction artistique.

Partager la logique d’analyse en Kotlin. Introduire une source PCM Android via AudioRecord, une source iOS via AVAudioEngine et une source de test lisant le WAV fourni. Garder l’estimation hors du thread de rendu. Sur permission refusée, interruption ou départ de l’écran, garder un état compréhensible et libérer la capture. Pour la 3D, tester Filament pour Android et RealityKit pour iOS avec une géométrie compatible ou un export commun. Isoler l’intégration sous une interface qui expose la sélection et la vis active.

## Estimation par frappe

Distinguer trois états de mesure sur le même écran :

- **Frappe captée.** L’impact déclenche immédiatement le retour visuel. La hauteur peut rester en calcul.
- **Fréquence exploitable.** Afficher une estimation provisoire en Hz près de la vis. Analyser plusieurs fenêtres de la décroissance après l’impact et vérifier si une résonance suffisamment étayée persiste.
- **Mesure non concluante.** Si les modes concurrents restent ambigus, ou si la frappe est saturée ou trop bruitée, demander de refrapper. Garder le signal de frappe captée et ne pas afficher de nouvelle valeur en Hz.

Comparer des candidats spectraux et les indices de périodicité utiles selon le signal. Vérifier les confusions d’octave. La résonance la plus forte et celle qui est la plus proche de la cible peuvent servir d’indices, mais aucun de ces critères ne suffit à lui seul. Documenter la bande de recherche, les fenêtres et les critères d’acceptation du prototype. Les présenter comme des paramètres d’essai. Une frappe donne au plus une estimation.

Le lot 0 se limite à une frappe à la fois près d’une vis. La mesure au centre, peaux libres, peut faire l’objet d’un essai séparé sans nouvel écran. Le contrôle complet des kits, la persistance, l’accordage des deux peaux et la stabilité entre frappes viendront dans les lots suivants.

## Cas d’essai

Le signal [frappes.wav](frappes.wav) est synthétique. Le [CSV](frappes.csv) indique les huit impacts et leurs fréquences attendues. Il sert de test de départ. Ajouter les cas suivants sous forme de signaux déterministes générés dans les tests ou de fichiers accompagnés de leurs valeurs attendues.

| Cas | Résultat attendu |
|---|---|
| WAV de référence | Détecter les huit impacts, produire au plus une estimation par impact et vérifier l’écart à chacune des fréquences attendues |
| Résonance secondaire dominante | Retenir la fréquence de référence lorsqu’elle reste identifiable malgré un pic concurrent plus fort. Tester aussi un concurrent proche du double et une composante inharmonique |
| Modes ambigus | Reconnaître la frappe, puis indiquer une mesure non concluante lorsque les informations disponibles ne permettent pas de départager les candidats |
| Fréquence éloignée de la cible | Afficher la fréquence détectée si elle est exploitable. Ne pas ramener la mesure vers la cible |
| Silence, bruit, saturation et décroissance | Ne pas accepter de valeur exploitable à partir du bruit seul ou d’une frappe saturée. Ne pas compter la décroissance comme une deuxième frappe |
| Tom réel près d’une vis | Sur un tom et une vis identifiés, étouffer la peau opposée et effectuer plusieurs frappes similaires. Comparer la résonance suivie à une mesure de référence prise avec le même geste. Relever les écarts, erreurs d’octave et rejets |

Pour les cas synthétiques, fixer et consigner la tolérance utilisée par le test. Sur le vrai tom, noter l’appareil, le placement du micro et la méthode de référence. Rapporter séparément les erreurs d’estimation, les mesures rejetées et les frappes manquées. Ne pas déduire la fiabilité sur batterie réelle des seuls signaux synthétiques.

## Délégation

L’agent principal pose les contrats de capture et de sélection 3D et documente les commandes du projet. Déléguer en parallèle le signal et le rendu une fois ces contrats écrits. L’agent audio intervient sur l’analyse partagée, ses tests et les adaptateurs de capture. L’agent 3D intervient sur les vues, leurs ressources et la sélection. L’agent principal intègre les retours. Demander ensuite une revue indépendante des critères à l’agent validation.

## Livraison

- Fournir la branche et le diff, les commandes et leurs résultats, ainsi que les étapes pour reproduire les essais sur Android et iOS.
- Tester la sélection de vis et la capture simultanées. Relever le délai attaque → signal → Hz, les frappes manquées ou comptées deux fois, les mesures non concluantes et les sélections de modes erronées.
- Sur appareils physiques, vérifier la permission, le retour après une interruption et la libération à la fermeture de l’écran. Évaluer la fluidité et l’estimation près d’une vis de tom avec l’autre peau étouffée.
- Conclure par l’état réel de chaque point. Un build, un test sur le WAV et un test au micro sont trois éléments de preuve différents. Si un appareil ou Xcode manque, identifier l’essai restant.
