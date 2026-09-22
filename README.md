# Traduction automatisée de rapports SAP BusinessObjects

## Contexte

Ce projet répond à un besoin de traduction de rapports SAP BusinessObjects dans plusieurs langues.

La traduction manuelle des libellés représente un travail répétitif et difficile à maintenir. L’objectif est donc d’automatiser le processus tout en conservant une terminologie cohérente entre les rapports.

> Ce dépôt ne contient aucune donnée métier, identifiant, configuration d’environnement ou information sensible.

## Données

Le programme traite les unités de traduction exportées depuis les rapports au format **XLIFF**.

Une base de traduction interne, au format texte séparé par tabulations, est utilisée en priorité. Elle contient un texte source et ses traductions de référence selon les langues disponibles.

Certains termes ou codes techniques sont gérés par des règles dédiées afin d’éviter les traductions incohérentes (exemple CA HTLP : Chiffre d'Affaires Hors Taxes Livraison et Pose).

## Démarche

L’application a été développée en **Java** avec une interface **JavaFX**.

Le processus est automatisé en plusieurs étapes :

1. connexion à l’environnement SAP BusinessObjects ;
2. récupération des contenus traduisibles d’un rapport ;
3. recherche d’une traduction existante dans la base de référence ;
4. application de règles spécifiques pour certains codes techniques ;
5. recours à un service de traduction lorsque nécessaire ;
6. mise à jour des traductions et export dans le rapport ;

L’application permet également d’alimenter la base de traduction à partir de traductions déjà validées dans un rapport existant.

## Résultats

L’outil centralise et automatise la traduction des champs d’un rapport, au lieu de les traiter un à un.

Il permet notamment de :

- réduire le temps nécessaire à la traduction d’un rapport ;
- réutiliser les traductions déjà validées ;
- harmoniser les libellés entre plusieurs rapports et langues ;
- suivre la progression du traitement ;
- limiter les manipulations manuelles dans l’outil de traduction SAP BO.

## Limites et pistes d’amélioration

La qualité des résultats dépend de la couverture et de la qualité de la base de traduction.

Les améliorations possibles incluent :

- enrichir progressivement la base avec les traductions validées ;
- ajouter de nouvelles langues ;
- étendre les règles de gestion des termes techniques ;
- renforcer les contrôles avant réimport ;
- ajouter un historique des traductions et des traitements réalisés.

## Technologies utilisées

- Java
- JavaFX
- SAP BusinessObjects
- XLIFF
- Base de traduction au format TSV
