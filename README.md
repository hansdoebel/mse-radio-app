# Radiyo

> [!IMPORTANT]
> This project was created as part of a university assignment and is **not actively maintained**.
> No further development or support is planned.

Eine interaktive Radio-Streaming-Anwendung für Android, die Zuhörer:innen und Moderator:innen in Echtzeit verbindet. Benutzer:innen können den aktuellen Titel sehen, Songs wünschen und Playlists sowie Moderator:innen bewerten. Moderator:innen verwalten die Warteschlange, genehmigen Anfragen und erhalten Live-Benachrichtigungen.

## Inhaltsverzeichnis

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Erste Schritte](#erste-schritte)
  - [Voraussetzungen](#voraussetzungen)
  - [Installation](#installation)
  - [Moderator-Konto erstellen](#moderator-konto-erstellen)
- [Repository-Struktur](#repository-struktur)
- [Umgebungsvariablen](#umgebungsvariablen)
- [Datenbankschema](#datenbankschema)
- [Tests](#tests)
- [Demo](#demo)
- [Ressourcen](#ressourcen)
- [Lizenz](#lizenz)

## Features

**Zuhörer:innen**
- Aktuell spielenden Song mit Metadaten anzeigen
- Songwünsche an den Radiosender senden
- Anfrageverlauf und Status verfolgen
- Playlists und Moderator:innen bewerten

**Moderator:innen**
- Dashboard mit Echtzeit-Statistiken
- Ausstehende Songwünsche verwalten
- Warteschlangenverwaltung und Song-Planung
- In-App-Benachrichtigungen für neue Anfragen und Bewertungen

## Tech Stack

| Ebene          | Technologie                                     |
|----------------|------------------------------------------------|
| Programmiersprachen        | Kotlin, Typescript                                   |
| UI-Framework   | Jetpack Compose mit Material 3                 |
| Architektur    | Model-View-ViewModel mit Repository-Pattern                    |
| Backend        | Convex (serverlose BaaS mit Echtzeit-Sync)     |
| Authentifizierung | Clerk                                       |
| Build-System   | Gradle 8.13.2             |
| Min SDK        | 30 (Android 11)                                |
| Target SDK     | 36                                             |

## Erste Schritte

### Voraussetzungen

- Android Studio Otter 2
- Node.js (für Convex CLI)
- Ein [Clerk](https://clerk.com)-Konto
- Ein [Convex](https://convex.dev)-Konto

### Installation

1. Repository klonen

```bash
git clone https://github.com/hansdoebel/mse-radio-app.git
cd mse-radio-app
```

2. Convex installieren

```bash
npm install
```

3. Clerk-Authentifizierung einrichten

   - Neue Anwendung unter [dashboard.clerk.com](https://dashboard.clerk.com) erstellen
   - Zu **JWT Templates** > **New template** > **Convex** navigieren
   - Template `convex` nennen (<ins>muss</ins> convex genannt werden)
   - Publishable Key kopieren

> [!NOTE]
> Außerdem unter "User & Authentication" -> "Email" -> "Verify at signup" deaktivieren      
> Und unter "Instance" -> "Settings" -> "Test mode" aktivieren

4. Umgebungsvariablen konfigurieren

   `local.properties` im Projektstammverzeichnis erstellen (falls nicht vorhanden) und hinzufügen:

   ```properties
   CLERK_PUBLISHABLE_KEY=pk_test_your_key_here
   ```

   `.env.local` im Projektstammverzeichnis erstellen und hinzufügen:

   ```
   CONVEX_DEPLOYMENT=dev:your-deployment
   CONVEX_URL=https://your-deployment.convex.cloud
   CLERK_JWT_ISSUER_DOMAIN=https://your-app.clerk.accounts.dev
   ```

5. Convex-Entwicklungsserver starten

```bash
npx convex dev
```

6. Datenbank mit Beispielsongs befüllen

```bash
npx convex run scripts/seed
```

7. Projekt in Android Studio öffnen und ausführen

### Moderator Konto erstellen

1. Neues Konto in der App registrieren
2. [Convex Dashboard](https://dashboard.convex.dev) öffnen
3. Zum Projekt navigieren > Data > `users`-Tabelle
4. `role` des Benutzers von `listener` auf `moderator` ändern
5. App neu bauen und ausführen

## Repository-Struktur

```
mse-radio-app/
├── app/src/main/java/com/example/radiyo/
│   ├── MainActivity.kt              # Einstiegspunkt
│   ├── RadiyoApplication.kt         # App-Initialisierung, Clerk-Setup
│   ├── data/
│   │   ├── model/                   # Datenklassen (Song, User, Rating, etc.)
│   │   ├── repository/              # Datenzugriffsschicht
│   │   └── remote/                  # Convex-Client-Konfiguration
│   ├── ui/
│   │   ├── RadiyoApp.kt             # Hauptnavigation
│   │   ├── screens/                 # UI-Screens (Login, NowPlaying, etc.)
│   │   ├── viewmodel/               # MVVM ViewModels
│   │   ├── components/              # Wiederverwendbare UI-Komponenten
│   │   └── theme/                   # Material 3 Theming
│   └── notification/                # In-App-Benachrichtigungssystem
├── convex/                          # Backend (TypeScript)
│   ├── schema.ts                    # Datenbankschema
│   ├── auth.config.ts               # Clerk JWT-Validierung
│   ├── users.ts                     # User Queries/Mutations
│   ├── songs.ts                     # Song-Katalog-Funktionen
│   ├── ratings.ts                   # Bewertungssystem
│   ├── songRequests.ts              # Anfrageverwaltung
│   ├── nowPlaying.ts                # Aktueller Titel-Status
│   ├── playlists.ts                 # Playlist-Verwaltung
│   └── scripts/seed.ts              # Datenbank-Seeding
├── gradle/libs.versions.toml        # Dependency Version Catalog
└── app/build.gradle.kts             # App-Build-Konfiguration
```

## Umgebungsvariablen

| Variable                  | Speicherort      | Beschreibung                         |
|---------------------------|------------------|--------------------------------------|
| `CLERK_PUBLISHABLE_KEY`   | local.properties | Clerk Frontend-API-Key               |
| `CONVEX_URL`              | .env.local       | Convex Deployment-URL                |
| `CONVEX_DEPLOYMENT`       | .env.local       | Convex Deployment-Identifier         |
| `CLERK_JWT_ISSUER_DOMAIN` | .env.local       | Clerk JWKS-Endpoint für JWT-Validierung |

## Datenbankschema

| Tabelle        | Beschreibung                                     |
|----------------|--------------------------------------------------|
| `users`        | Benutzerprofile mit Rollen (listener/moderator)  |
| `songs`        | Song-Katalog mit Titel, Künstler, Album, Dauer   |
| `playlists`    | Radiosender-Playlists                            |
| `playlistSongs`| Many-to-Many-Beziehung für Playlists             |
| `nowPlaying`   | Aktuell spielender Titel                         |
| `ratings`      | Benutzerbewertungen für Playlists und Moderatoren|
| `songRequests` | Songwünsche mit Status-Tracking                  |

## Tests

Das Projekt enthält Unit-Tests für die Validierungslogik und Geschäftsregeln.

### Tests ausführen

```bash
./gradlew test
```

### Testübersicht

| Testklasse              | Beschreibung                                      |
|-------------------------|---------------------------------------------------|
| `EmailValidatorTest`    | E-Mail-Validierung (Format, Whitespace-Trimming)  |
| `PasswordRulesTest`     | Passwort-Validierung (Mindestlänge, Leerzeichen)  |
| `RatingRulesTest`       | Bewertungsregeln (Wertebereich 1-5, Kommentare)   |
| `PlaylistRulesTest`     | Playlist-Sanitierung (leere Namen, Duplikate)     |
| `InAppNotificationTest` | Benachrichtigungsformatierung (Titel, Nachrichten)|

### Teststruktur

```
app/src/test/java/com/example/radiyo/
├── EmailValidatorTest.kt
├── InAppNotificationTest.kt
├── PasswordRulesTest.kt
├── PlaylistRulesTest.kt
└── RatingRulesTest.kt
```

## Demo

#### Benutzer:innen Anmeldung und Ansicht der Titelinformationen:

[![Anmeldung und Titelinformationen](https://vumbnail.com/1156879794.jpg)](https://vimeo.com/1156879794)

#### Bewertung von Moderator:innen:

[![Bewertung von Moderator:innen](https://vumbnail.com/1156879821.jpg)](https://vimeo.com/1156879821)

#### Bewertung einer Playlist:

[![Playlist bewerten](https://vumbnail.com/1156879842.jpg)](https://vimeo.com/1156879842)

#### Titelwunsch:

[![Songwunsch](https://vumbnail.com/1156879870.jpg)](https://vimeo.com/1156879870)

## Ressourcen

### Software
- [Android Studio](https://developer.android.com/studio/intro)
- [Kotlin Dokumentation](https://kotlinlang.org/docs/home.html)
- [Claude Code](https://claude.com/de-de/product/claude-code)
- [Node.js Dokumentation](https://nodejs.org/docs/latest/api/)

### Services
- [Convex Android Dokumentation](https://docs.convex.dev/client/android)
- [Clerk Android Quickstart](https://clerk.com/docs/android/getting-started/quickstart)

### Sonstiges
- [Git Referenz](https://git-scm.com/docs)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Cap - Open Source Screen Recordings](https://cap.so/)
- [GitHub – Basic writing and formatting syntax](https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax)

## Lizenz

MIT
