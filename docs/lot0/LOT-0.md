# Lot 0 — Faisabilité Kotlin

## Mission

Un écran Compose affiche un tom en 3D légèrement de biais depuis le dessus, avec une vis tactile et un repère entre les deux vis du haut. Il donne un retour « Frappe captée » et affiche la fréquence provisoire en Hz lorsqu’elle est exploitable. Le reste du produit est décrit dans [la spec](../../SPEC.md). La maquette filaire aide à retrouver les états et l’image fournit la direction artistique.

Partager la logique d’analyse en Kotlin. Introduire une source PCM Android via AudioRecord, une source iOS via AVAudioEngine et une source de test lisant le WAV fourni. Garder l’estimation hors du thread de rendu. Sur permission refusée, interruption ou départ de l’écran, garder un état compréhensible et libérer la capture. Pour la 3D, tester Filament pour Android et RealityKit pour iOS avec une géométrie compatible ou un export commun. Isoler l’intégration sous une interface qui expose la sélection et la vis active.

Le signal [frappes.wav](frappes.wav) est synthétique. Le [CSV](frappes.csv) indique les huit impacts et leurs fréquences attendues. Ajouter les tests de détection sur le fichier, de silence sans déclenchement et de séparation des frappes. La capture sur des fûts réels sert à vérifier la pertinence de l’estimation. Le lot 0 se limite à une frappe à la fois. Le contrôle complet des kits, la persistance, l’accordage des deux peaux et la stabilité entre frappes viendront dans les lots suivants.

## Délégation

L’agent principal pose les contrats de capture et de sélection 3D et documente les commandes du projet. Déléguer en parallèle le signal et le rendu une fois ces contrats écrits. L’agent audio intervient sur l’analyse partagée, ses tests et les adaptateurs de capture. L’agent 3D intervient sur les vues, leurs ressources et la sélection. L’agent principal intègre les retours. Demander ensuite une revue indépendante des critères à l’agent validation.

## Livraison

- Fournir la branche et le diff, les commandes et leurs résultats, ainsi que les étapes pour reproduire les essais sur Android et iOS.
- Tester la sélection de vis et la capture simultanées. Relever le délai attaque → signal → Hz et les frappes manquées ou comptées deux fois.
- Sur appareils physiques, vérifier la permission, le retour après une interruption et la libération à la fermeture de l’écran. Évaluer la fluidité et l’estimation près d’une vis de tom avec l’autre peau étouffée.
- Conclure par l’état réel de chaque point. Un build, un test sur le WAV et un test au micro sont trois éléments de preuve différents. Si un appareil ou Xcode manque, identifier l’essai restant.
