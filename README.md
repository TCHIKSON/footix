# Footix TV

Application Android TV (Kotlin / Leanback) qui affiche le catalogue football
d'une API distante et lit les flux HLS avec libVLC. Aucun compte, aucun profil :
le terminal est l'utilisateur.

## Fonctionnement

1. Au lancement, appel de `GET {BASE_URL}/api/index.php?table=matches`.
2. Seuls les matchs dont le champ `sport` contient `football` sont conserves.
3. Les matchs sont regroupes par competition, une rangee par competition, plus
   une rangee `En direct` en tete.
4. A la selection d'une carte, appel de
   `GET {BASE_URL}/api/?action=stream&slug={slug}`.
5. La premiere URL `.m3u8` du tableau `sources` est tronquee juste apres
   `.m3u8`, puis envoyee au lecteur libVLC.

Pendant le chargement du catalogue, le logo bat au centre de l'ecran et le
bandeau de titre reste vide ; le logo n'apparait en haut a droite qu'une fois
les rangees affichees. Avant meme le premier rendu, le fond de fenetre du theme
`Theme.Footix.Splash` evite l'ecran noir au demarrage.

## Installation sur la TV

L'APK a installer est `app/build/outputs/apk/debug/app-universal-debug.apk`.
Il fonctionne sur toute TV Android, quelle que soit son architecture.

### Par adb, en reseau

1. Sur la TV : Parametres > Preferences de l'appareil > A propos, puis cliquer
   7 fois sur **Build** pour activer le mode developpeur.
2. Parametres > Preferences de l'appareil > Options pour les developpeurs :
   activer **Debogage ADB**.
3. Relever l'adresse IP de la TV (Parametres > Reseau). Le PC doit etre sur le
   meme reseau.
4. Depuis le PC :

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb connect 192.xxx.x.xx:5555
& $adb install -r "app\build\outputs\apk\debug\app-universal-debug.apk"
```

A la premiere connexion, la TV demande d'autoriser le debogage : accepter en
cochant « Toujours autoriser ». Tant que `& $adb devices` affiche `unauthorized`
au lieu de `device`, l'installation est bloquee.

### Sans adb

Copier le meme APK sur une cle USB et l'installer depuis un explorateur de
fichiers de la TV, apres avoir autorise les sources inconnues. L'application
« Send Files to TV » fait la meme chose par Wi-Fi.

### Mettre a jour

**Incrementer `versionCode` dans `app/build.gradle.kts` a chaque livraison.**
Le lanceur Android TV met en cache la banniere de chaque application et ne la
relit que si ce numero change : sans cela, l'ancienne vignette reste affichee
apres la mise a jour. Si elle persiste malgre tout, redemarrer la TV.

## Configuration

Tout se regle dans un seul fichier :
`app/src/main/java/com/footix/tv/core/AppConfig.kt`

### API et catalogue

| Constante | Role |
| --- | --- |
| `BASE_URL` | Domaine de l'API |
| `MATCHES_PATH` / `STREAM_PATH` | Chemins des deux endpoints |
| `SPORT_KEYWORD` | Mot-cle du filtre sport |
| `HTTP_TIMEOUT_SECONDS` / `HTTP_USER_AGENT` | Client HTTP |
| `CATALOG_TTL_MS` | Duree du cache memoire du catalogue |
| `SHOW_LIVE_ROW` | Affiche ou non la rangee « En direct » |
| `LOADING_PULSE_DURATION_MS` | Vitesse du battement du logo au chargement |

### Lecture

| Constante | Role |
| --- | --- |
| `STRIP_STREAM_QUERY` | Troncature apres `.m3u8` (a passer a `false` si le CDN exige la signature `?md5=...&expires=...`) |
| `PLAYER_LOW_LATENCY` | Demarre plus pres du direct, au prix de gels plus frequents |
| `PLAYER_LIVE_DELAY_MS` | Retard vise au demarrage |
| `PLAYER_NETWORK_CACHING_MS` / `PLAYER_LOW_LATENCY_CACHING_MS` | Tampon reseau libVLC selon le mode choisi |
| `PLAYER_MAX_DELAY_MS` | Retard maximum tolere avant resynchronisation (`0` desactive) |
| `PLAYER_DELAY_CHECK_INTERVAL_MS` | Frequence de controle du retard |
| `PLAYER_MAX_RETRIES` / `PLAYER_RETRY_DELAY_MS` | Politique de relance automatique |
| `PLAYER_REFRESH_URL_AT_ATTEMPT` | Tentative a laquelle l'URL est redemandee au serveur |
| `PLAYER_SEEK_STEP_MS` / `PLAYER_OVERLAY_TIMEOUT_MS` | Telecommande et habillage |
| `PLAYER_USER_AGENT` / `PLAYER_REFERER` | En-tetes HTTP si le CDN les exige |

## Retard face au direct

Le protocole HLS impose de demarrer au moins trois durees de segment avant le
bord du direct : avec des segments de 4 s, cela fait environ 12 s de retard
structurel, communs a tous les lecteurs. On ne peut pas descendre sous la duree
d'un segment, la source ne publiant un segment qu'une fois ecrit en entier.

Ce retard ne se rattrape jamais seul : chaque gel et chaque pause eloignent la
lecture du direct definitivement. Le lecteur surveille donc cette derive toutes
les `PLAYER_DELAY_CHECK_INTERVAL_MS` et recharge le flux des que le retard
estime depasse `PLAYER_MAX_DELAY_MS`.

## Reprise apres coupure

| Etape | Action |
| --- | --- |
| Tentatives 1 et 2 | relance sur la meme URL |
| Tentative 3 | nouvel appel a l'API, lecture de l'URL renvoyee |
| Tentative 4 | relance sur l'URL en cours |
| Apres la derniere | ultime appel a l'API avant d'abandonner |

Redemander l'URL a son interet : les flux ne sont pas tous servis par la meme
machine, et l'adresse d'un match peut changer en cours de rencontre. Le
controleur libVLC ignore tout du reseau : il expose une interface `UrlResolver`
que `PlayerActivity` remplit avec le depot, le slug du match transitant dans
l'intent aux cotes de l'URL.

## Structure

```
core/       configuration, erreurs, etat de chargement, journal, cablage
domain/     regles metier : filtre football, regroupement, choix de la source
data/       DTO, parsing JSON, client HTTP, depots (cache)
ui/home/    catalogue Leanback (rangees, cartes, ViewModel)
ui/common/  vues partagees, dont le logo battant
ui/player/  lecteur : PlayerActivity + VlcPlayerController (libVLC isole)
```

Chaque couche ne connait que la couche du dessous. Pour brancher une autre API,
seul `data/` change ; pour modifier l'aspect, seul `ui/` change.

## Build

```
gradlew assembleDebug        # APK de test
gradlew testDebugUnitTest    # tests de la logique metier
gradlew assembleRelease      # APK non signe, R8 actif
```

APK generes dans `app/build/outputs/apk/`. Le split ABI produit un APK par
architecture plus un APK universel, le plus simple a installer.

Les ABI compilees sont `armeabi-v7a` et `arm64-v8a` : l'emulateur Android TV
x86_64 ne peut donc pas charger libVLC. Ajouter `x86_64` aux `abiFilters` et
aux `splits` pour tester dans l'emulateur.

Pour diffuser l'application hors de votre reseau, il faut une cle de signature
(`keytool -genkeypair -keystore footix.jks -alias footix -keyalg RSA -validity
10000`) et une `signingConfig` dans `app/build.gradle.kts`, les mots de passe
etant places dans un `keystore.properties` exclu de git. Une application signee
avec une cle differente ne peut pas se mettre a jour par-dessus la version
debug : il faut la desinstaller d'abord.

## Journalisation

```
adb logcat -s Footix
```

Le tag `Footix` trace les appels HTTP, le nombre de matchs retenus, l'URL de
flux choisie et les resynchronisations. En cas d'echec au demarrage, le motif
technique est aussi affiche sur la carte de message, a l'ecran.

## Ressources graphiques

`tv_banner.jpeg` est l'image source du projet : elle sert de banniere au
lanceur TV. `app_logo.png` (360x136) en est un recadrage, utilise pour le badge
du catalogue, le logo de chargement et l'ecran de demarrage. Les cinq
`ic_launcher.png` (48 a 192 px) sont derives de l'embleme circulaire.

Pour changer l'identite visuelle, remplacer `tv_banner.jpeg` puis regenerer les
autres fichiers a partir de lui. La banniere du lanceur Android TV est attendue
en 320x180 : une image d'un autre rapport sera rognee ou deformee.
