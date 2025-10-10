# Jeu de la vie – Éditeur collaboratif

Un éditeur collaboratif multi-utilisateurs pour le Jeu de la vie de Conway, développé en Java avec le framework Spark et PostgreSQL.

## À propos du Jeu de la vie

Le **Jeu de la vie** est un automate cellulaire imaginé par le mathématicien **John Conway**. Malgré sa simplicité, il est **Turing-complet** et peut simuler n’importe quel algorithme.

**En savoir plus :**
- [Wikipédia – Règles](https://fr.wikipedia.org/wiki/Jeu_de_la_vie)
- [Vidéo Science étonnante](https://www.youtube.com/watch?v=S-W0NX97DB0)
- [Le Jeu de la vie qui se simule lui-même](https://www.youtube.com/watch?v=xP5-iIeKXE8)

## Fonctionnalités

- **Grille interactive** : cliquez sur les cellules pour les activer/désactiver  
- **Mises à jour en temps réel** : plusieurs utilisateurs peuvent modifier la même grille simultanément  
- **Gestion des transactions** : enregistrez ou annulez vos changements avant de les valider  
- **Import de motifs (RLE)** : chargez des motifs à partir d’un lien  
- **Contrôles de simulation** :
  - Passez manuellement à la génération suivante  
  - Lecture automatique avec vitesse ajustable  
  - Effacer la grille  
  - Rafraîchir l’affichage  
- **Zoom et déplacement** : naviguez sur de grandes grilles avec la souris ou les clés "+" et "-"

## Pile technologique

- **Backend** : Java 8+, Spark Framework  
- **Base de données** : PostgreSQL  
- **Frontend** : JavaScript pur, Canvas HTML5  
- **Outil de build** : Gradle  
- **Moteur de template** : FreeMarker

## Prérequis

- Java JDK 8 ou version supérieure  
- PostgreSQL  
- Gradle (ou utilisez le wrapper inclus)

## Configuration de la base de données

### 1. Installation de PostgreSQL

```bash
sudo apt install postgresql
```

### 2. Création et configuration de la base

Connectez-vous à PostgreSQL en tant qu’utilisateur `postgres` :

```bash
sudo su postgres
psql
```

Créez votre utilisateur (remplacez `votre_login` et `votre_mot_de_passe`) :

```sql
CREATE USER votre_login WITH PASSWORD 'votre_mot_de_passe';
```

Créez la base de données :

```sql
CREATE DATABASE life;
```

Attribuez les droits :

```sql
GRANT ALL PRIVILEGES ON DATABASE life TO votre_login;
```

Quittez psql avec `\q`.

### 3. Tester la connexion

```bash
psql -U votre_login -d life
```

## Configuration de l’application

Modifiez le fichier `src/com/uca/dao/_Connector.java` et mettez à jour les valeurs suivantes :

```java
private static String url = "jdbc:postgresql://localhost/life";
private static String user = "votre_login";
private static String passwd = "votre_mot_de_passe";
```

## Lancement de l’application

### Compilation et exécution

```bash
./gradlew run
```

Ou si vous avez Gradle installé globalement :

```bash
gradle run
```

### Accès à l’application

Ouvrez votre navigateur à l’adresse :

```
http://localhost:8081/
```

## Structure du projet

```
projet-life/
├── src/
│   ├── com/uca/
│   │   ├── StartServer.java          # Classe principale du serveur et routes
│   │   ├── core/
│   │   │   └── GridCore.java         # Logique métier
│   │   ├── dao/
│   │   │   ├── _Connector.java       # Connexion à la base de données
│   │   │   ├── _Initializer.java     # Initialisation de la base
│   │   │   └── GridDAO.java          # Couche d’accès aux données
│   │   ├── entity/
│   │   │   ├── CellEntity.java       # Modèle de cellule
│   │   │   └── GridEntity.java       # Modèle de grille
│   │   └── gui/
│   │       ├── IndexGUI.java         # Contrôleur de la vue
│   │       └── _FreeMarkerInitializer.java
│   └── main/resources/
│       ├── static/
│       │   ├── life.js               # JavaScript du frontend
│       │   ├── style.css             # Feuille de style
│       │   └── loading.svg           # Indicateur de chargement
│       └── views/
│           └── index.ftl             # Template HTML
├── build.gradle                       # Configuration Gradle
└── README.md                          # Fichier de documentation
```

## API

| Méthode | Endpoint | Description |
|----------|-----------|-------------|
| GET | `/` | Page principale |
| GET | `/grid` | Récupère l’état actuel de la grille |
| PUT | `/grid/change` | Active/désactive une cellule |
| POST | `/grid/save` | Valide les modifications |
| POST | `/grid/cancel` | Annule les modifications |
| PUT | `/grid/rle` | Importe un motif RLE depuis une URL |
| POST | `/grid/empty` | Vide la grille |
| POST | `/grid/next` | Calcule la génération suivante |

## Utilisation

### Commandes de base

- **Clic** sur une cellule : active/désactive son état  
- **Rafraîchir** : recharge la grille  
- **Vider** : supprime toutes les cellules  
- **Suivant** : calcule la génération suivante  
- **Lecture/Pause** : avance automatiquement selon la vitesse choisie

### Gestion des transactions

Chaque onglet du navigateur dispose de sa propre session.  
Les modifications sont isolées jusqu’à ce que vous :
- **Enregistriez** : pour valider vos changements dans la base  
- **Annuliez** : pour les annuler

### Import de motifs

1. Rendez-vous sur [copy.sh/life/examples/](https://copy.sh/life/examples/)  
2. Copiez le lien du fichier RLE dans la section “Pattern files”  
3. Collez ce lien dans le champ RLE de l’application  
4. Cliquez sur **Importer**

Exemple d’URL :  
`https://copy.sh/life/examples/glider.rle`

## Schéma de la base de données

L’application utilise une seule table :

```sql
CREATE TABLE grid (
    x INT NOT NULL,
    y INT NOT NULL,
    state INT NOT NULL,
    PRIMARY KEY (x, y)
);
```

- **x, y** : coordonnées de la cellule  
- **state** : 0 (morte) ou 1 (vivante)

## Commandes PostgreSQL utiles

Dans `psql` :

- `\q` — quitter  
- `\dt` — lister les tables  
- `\c nom_base` — se connecter à une autre base  
- `BEGIN` — démarrer une transaction (auto-commit activé par défaut)

## Gestion des transactions

L’application utilise le niveau d’isolation `READ_COMMITTED` de PostgreSQL pour gérer les accès concurrents de plusieurs utilisateurs.

## Contribution

Ce projet est réalisé à des fins pédagogiques, pour apprendre :
- La gestion de transactions en base de données  
- L’accès concurrent aux données  
- Les applications collaboratives en temps réel

## Licence

Ce projet est éducatif et libre d’usage pour l’apprentissage et la démonstration.  
Aucune licence spécifique n’a été définie.

## Remerciements

- John Conway, créateur du Jeu de la vie  
- [copy.sh](https://copy.sh/life/examples/) pour sa base de motifs RLE
