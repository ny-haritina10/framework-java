# Framework-Java

Un framework Java construit de zéro.

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-green)

## Table des Matières

- [Description](#description)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Utilisation](#utilisation)
- [Licence](#licence)
- [Contact](#contact)

## Description

Framework-Java est un framework MVC développé en Java à partir de zéro. Il permet de créer des applications web robustes et modulaires en suivant les bonnes pratiques de programmation. 

## Prérequis

- Java JDK 8 ou supérieur
- Serveur d'applications compatible avec les servlets (comme Apache Tomcat)

## Installation

1. Clonez le dépôt
    ```sh
    git clone https://github.com/ny-haritina10/framework-java.git
    ```
2. Naviguez dans le répertoire du projet
    ```sh
    cd framework-java
    ```
3. Compilez le projet avec le script run.bat
    ```sh
    run.bat
    ```
4. Placer le fichier JAR `framework.jar` dans `WEB-INF/lib` 

## Utilisation

1. Dans le fichier `web.xml`, ajoutez/modifiez la balise suivante :
    ```xml
    <param-value>[votre nom de package du contrôleur ici]</param-value>
    ```
2. Déployez votre application sur un serveur compatible avec les servlets.
3. Accédez à votre application via le navigateur en utilisant l'URL appropriée.
4. Utilisation de Session
    ### Méthode 1 : Ajout de la classe session en tant qu'attribut du Controller
    - Ajouter un attribut de type `Session` dans le votre Controller
    - Utiliser la Session avec les méthodes `add`, `get`, `delete`  

    ### Méthode 2 : Ajout de la classe session en tant qu'argument de la méthode du Controller
    - Ajouter un argument de type Session dans la fonction du Controller 
    - Utiliser la Session avec les méthodes `add`, `get`, `delete`  

## Licence

Distribué sous la licence MIT. Voir `LICENSE` pour plus d'informations.

## Contact

RABEMANANTSOA Ny Haritina ETU002716