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

## Configuration

Tout se regle dans un seul fichier :
`app/src/main/java/com/footix/tv/core/AppConfig.kt`

| Constante | Role |
| --- | --- |
| `BASE_URL` | Domaine de l'API (a remplacer par le domaine reel) |
| `MATCHES_PATH` / `STREAM_PATH` | Chemins des deux endpoints |
| `SPORT_KEYWORD` | Mot-cle du filtre sport |
| `CATALOG_TTL_MS` | Duree du cache memoire du catalogue |
| `SHOW_LIVE_ROW` | Affiche ou non la rangee "En direct" |
| `STRIP_STREAM_QUERY` | Troncature apres `.m3u8` (a passer a `false` si le CDN exige la signature `?md5=...&expires=...`) |
| `PLAYER_NETWORK_CACHING_MS` | Tampon reseau libVLC (monter si le flux saccade) |
| `PLAYER_MAX_RETRIES` / `PLAYER_RETRY_DELAY_MS` | Politique de relance automatique |
| `PLAYER_USER_AGENT` / `PLAYER_REFERER` | En-tetes HTTP si le CDN les exige |

## Structure

```
core/       configuration, erreurs, etat de chargement, cablage des dependances
domain/     regles metier : filtre football, regroupement, choix de la source
data/       DTO, parsing JSON, client HTTP, depots (cache)
ui/home/    catalogue Leanback (rangees, cartes, ViewModel)
ui/player/  lecteur : PlayerActivity + VlcPlayerController (libVLC isole)
```

Chaque couche ne connait que la couche du dessous. Pour brancher une autre API,
seul `data/` change ; pour modifier l'aspect, seul `ui/` change.

## Build

```
gradlew assembleDebug        # APK de test
gradlew testDebugUnitTest    # tests de la logique metier
gradlew assembleRelease      # APK signe a configurer, R8 active
```

APK generes dans `app/build/outputs/apk/`. Le split ABI produit un APK par
architecture plus un APK universel (`app-universal-debug.apk`), le plus simple
a installer par `adb install`.

## Ressources graphiques

`tv_banner.png` (banniere du lanceur TV), `app_logo.png` (badge du catalogue,
fond transparent) et les `ic_launcher.png` sont generes depuis
`Gemini_Generated_Image_bp5hh5bp5hh5bp5h.jpeg`.
