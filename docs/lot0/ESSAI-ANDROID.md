# Essai physique Android — 23 novembre 2026

## Appareil et conditions

Pixel 9 Pro XL, Android **17**, API **37**, téléphone USB physique identifié par ADB. Le microphone reçoit du son réel ; aucun WAV n’est diffusé ni injecté dans cet essai. Filament et AudioRecord fonctionnent sur le même écran. Source PCM réelle à 48 kHz selon `CLOCK`.

L’opérateur fournit une **caisse claire de 14 pouces, timbre débrayé**. Cet essai diffère du critère lot 0 sur un vrai **tom**. Le téléphone à environ 30 cm et l’étouffement de la peau opposée pour la deuxième série ont été demandés ; la distance exacte, les peaux, la pièce et une fréquence de référence indépendante n’ont pas été fournis. Pas de mesure d’erreur absolue ni de qualification des résonances/octaves sans cette référence.

## Séquence exploratoire

L’opérateur précise après l’essai qu’il n’a pas compté les frappes : il a commencé au centre puis frappé près de trois ou quatre vis.

- Session 23, **28 détections, 20 estimations, 8 rejets** (7 `AmbiguousModes`, 1 `Noise`).
- Groupes de fréquences observés autour de **272 Hz, 262 Hz et 492 Hz**, avec changements de zone déclarés.
- Aucun nombre de frappes manquées/doubles ne peut être déduit de cette série non comptée. Aucune des trois résonances n’est déclarée correcte par simple cohérence.
- Trace et résumé : `preuves/pixel-serie1.log` / `.json`.

## Dix frappes près d’une même vis

L’opérateur confirme **« 10 fait »** après la consigne : dix frappes douces près de la même vis, sans changer la sélection, peau opposée étouffée, espacées d’environ trois secondes. Les dix attaques correspondantes sont espacées d’environ deux secondes dans la trace.

- Session 38 : **10 estimations pour les 10 frappes annoncées**, de **270,7935 à 271,1057 Hz**, médiane **270,9144 Hz**.
- Une **onzième détection est rejetée `Noise`**, 5,455 s après la dixième détection. Elle ne produit aucune estimation supplémentaire. Sa source n’est pas identifiée par un enregistrement indépendant.
- Ce relevé est compatible avec une estimation par frappe de la série. Il ne permet pas d’exclure formellement une compensation frappe manquée/double sans séquence filmée. Le surplus de détection rejeté est signalé séparément.
- Aucun retour explicite de l’opérateur sur les saccades ; pas de mesure FPS du rendu de téléphone revendiquée.
- Trace et résumé : `preuves/pixel-serie2.log` / `.json`.

## Temps de diagnostic, sans assimilation à l’impact physique

Sur les 11 événements de la session 38, début du pas RMS déclencheur → tick de l’horloge Compose de l’état de détection : médiane **41,36 ms**, plage **23,99–51,82 ms**. Sur les 10 estimations, même origine → tick de l’état Hz : médiane **396,73 ms**, plage **389,98–406,90 ms**.

Ces chiffres sont des **temps de diagnostic**. `withFrameNanos` ne prouve pas l’affichage effectif et le début du pas RMS n’est pas nécessairement le début de l’attaque. L’impact physique → retour réellement affiché et impact → Hz réellement affichés n’ont pas été filmés et restent à mesurer. Voir [METROLOGIE.md](METROLOGIE.md).

## Rendu et cycle de vie

- Huit vis sélectionnées par picking natif sur le Pixel ; coque, cercle inférieur, logo et halo observés dans `preuves/android-pixel.png`.
- AudioRecord actif avec la scène ; changement de sélection pendant la capture crée une session distincte.
- Passage en arrière-plan : app-op micro inactif ; retour avec `Capture interrompue`, reprise explicite.
- Fermeture de l’écran : micro inactif ; réouverture avec micro arrêté, picking natif à nouveau fonctionnel. Voir `preuves/pixel-lifecycle.log`.
- Permission accordée sur le téléphone, puis contrôle de refus/réautorisation natifs et arrêt final : `preuves/pixel-permissions.log`, `preuves/pixel-permission-denied.png`.
- Le premier helper de picking finit avec `StopIteration` en cherchant « Activer le micro » : l’opérateur avait déjà activé le micro. Les huit sélections précédentes avaient réussi et l’app-op confirmait une capture active. Ce défaut de synchronisation du helper n’est pas une exception de l’app ; les contrôles de cycle de vie suivants ont leur propre journal.
- Appel téléphonique et changement de route audio physique non exécutés. Aucun essai sur iPhone physique.

Le PCM a été traité en mémoire. Seuls événements, Hz et horodatages ont été conservés pour cette livraison. Aucune référence de fût n’a été enregistrée.
