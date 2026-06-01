# Music Manager Microservice

A Spring Boot REST API for managing artists, albums, songs, and playlists with ZIP file upload support for bulk import.

## Tech Stack

- **Spring Boot** 3.2.0
- **Java 17**
- **JPA/Hibernate** for ORM
- **H2 Database** (in-memory, default for dev) or **PostgreSQL** (for production)
- **Lombok** for boilerplate reduction
- **JAudiotagger** for reading audio file metadata
- **Maven** for dependency management

## Features

- **REST API** for CRUD operations (Artists, Albums, Songs, Playlists)
- **HTML Web UI** (Thymeleaf) for browsing, upload, and playlist management
- **Search** by artist name, album title, song title, or playlist name
- **Playlist Management** for creating and managing custom song collections
- **Bulk Import** via ZIP file containing audio files (MP3, FLAC, OGG, WAV, M4A)
- Automatic tag reading from audio files (ID3 tags)
- Album cover extraction from embedded artwork
- Input validation and error handling
- SQL injection prevention via JPA parameter binding
- JSON API responses (mobile-first)
- Pagination for albums with covers and songs
- Comprehensive logging with SLF4J
- **ZIP Export** - Download albums or playlists as ZIP (includes all audio files + generated M3U playlist)

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker (optional, for running PostgreSQL)

### Running the Application

From the project root (directory containing `pom.xml`):

```bash
mvn spring-boot:run
```

The service starts on **http://localhost:8081**

Access Points:
- **Web UI**: http://localhost:8081/
- **REST API**: http://localhost:8081/api/*
- **H2 Console**: http://localhost:8081/h2-console

- JDBC URL: `jdbc:h2:mem:musicdb`
- Username: `sa`
- Password: (empty)

### Database Configuration

By default, the service uses the embedded **H2** in-memory database (perfect for development). The console is at `/h2-console` with the credentials above.

#### Connecting to PostgreSQL

For persistent storage or production use:

1. **Add the PostgreSQL driver** to `pom.xml` (alongside the H2 dependency):

```xml
   <dependency>
   	<groupId>org.postgresql</groupId>
   	<artifactId>postgresql</artifactId>
   	<scope>runtime</scope>
   </dependency>
```

2. **Configure connection** – either edit `application.properties`

```properties
# PostgreSQL datasource
spring.datasource.url=jdbc:postgresql://localhost:5432/musicdb
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=musicuser
spring.datasource.password=secret

# Hibernate settings for Postgres
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

**One-liner Postgres via Docker:**

```bash
docker run -d --name music-postgres \
  -e POSTGRES_DB=musicdb -e POSTGRES_USER=musicuser -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 postgres:16-alpine
```

Update the JDBC URL if your Postgres runs on a different host/port. Hibernate will auto-create the tables (see "Database Schema" below).

**Local Postgres**
```bash
sudo -u postgres psql

CREATE USER musicuser WITH PASSWORD 'secret';

CREATE DATABASE musicdb;

ALTER DATABASE musicdb OWNER TO musicuser;

GRANT ALL PRIVILEGES ON DATABASE musicdb TO musicuser;

GRANT ALL ON SCHEMA public TO musicuser;

ALTER SCHEMA public OWNER TO musicuser;
```

## API Endpoints

All endpoints return JSON in the format:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "errors": null
}
```

### Artists

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/artists` | Create a new artist |
| GET | `/api/artists/{id}` | Get artist by ID |
| GET | `/api/artists` | Get all artists (paginated) |
| GET | `/api/artists/search?q={query}` | Search artists by name |
| PUT | `/api/artists/{id}` | Update artist |
| DELETE | `/api/artists/{id}` | Delete artist |

**Example - Create Artist:**
```bash
curl -X POST http://localhost:8081/api/artists \
  -H "Content-Type: application/json" \
  -d '{"name":"The Beatles","biography":"Legendary rock band"}'
```

### Albums

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/albums` | Create a new album |
| GET | `/api/albums/{id}` | Get album by ID |
| GET | `/api/albums` | Get all albums (paginated) |
| GET | `/api/albums/with-covers` | Get albums with cover images (paginated) |
| GET | `/api/albums/search/title?q={query}` | Search albums by title |
| GET | `/api/albums/search/artist?q={query}` | Search albums by artist name |
| PUT | `/api/albums/{id}` | Update album |
| PATCH | `/api/albums/{id}/genere?genere={genere}` | Update album genere only |
| DELETE | `/api/albums/{id}` | Delete album |
| POST | `/api/albums/{id}/cover` | Upload/update album cover image |
| GET | `/api/albums/{id}/cover` | Download album cover image |
| DELETE | `/api/albums/{id}/cover` | Delete album cover image |
| GET | `/api/albums/{id}/download` | Download entire album as ZIP (audio files + M3U playlist) |

**Example - Create Album:**
```bash
curl -X POST http://localhost:8081/api/albums \
  -H "Content-Type: application/json" \
  -d '{"title":"Abbey Road","releaseYear":1969,"artistId":1}'
```

**Update genere only:**
```bash
curl -X PATCH "http://localhost:8081/api/albums/1/genere?genere=Rock"
```

**Upload album cover:**
```bash
curl -X POST "http://localhost:8081/api/albums/1/cover" \
  -F "file=@/path/to/cover.jpg"
```

**Download album cover:**
```bash
curl -X GET "http://localhost:8081/api/albums/1/cover" --output cover.jpg
```

**Paginated albums with covers:**
```bash
curl -X GET "http://localhost:8081/api/albums/with-covers?page=0&size=20&sort=id,desc"
```

### Songs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/songs` | Create a new song |
| GET | `/api/songs/{id}` | Get song by ID |
| GET | `/api/songs` | Get all songs (paginated) |
| GET | `/api/songs/search/title?q={query}` | Search songs by title |
| GET | `/api/songs/search/album?q={query}` | Search songs by album title |
| GET | `/api/songs/search/artist?q={query}` | Search songs by artist name |
| PUT | `/api/songs/{id}` | Update song |
| DELETE | `/api/songs/{id}` | Delete song |

**Example - Create Song:**
```bash
curl -X POST http://localhost:8081/api/songs \
  -H "Content-Type: application/json" \
  -d '{"title":"Come Together","trackNumber":1,"durationSeconds":259,"genere":"Rock","albumId":1}'
```

**Example - Get Song Response (after ZIP upload):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Come Together",
    "trackNumber": 1,
    "durationSeconds": 259,
    "genere": "Rock",
    "filePath": "/tmp/music-extracted/20240429_211739_1234abcd/The Beatles/Abbey Road/01 - Come Together.mp3",
    "album": {
      "id": 1,
      "title": "Abbey Road",
      "artist": {
        "id": 1,
        "name": "The Beatles"
      }
    }
  }
}
```

### Playlists

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/playlists` | Create a new playlist |
| GET | `/api/playlists/{id}` | Get playlist by ID |
| GET | `/api/playlists` | Get all playlists (paginated) |
| PUT | `/api/playlists/{id}` | Update playlist |
| DELETE | `/api/playlists/{id}` | Delete playlist |
| POST | `/api/playlists/{playlistId}/songs/{songId}` | Add song to playlist |
| DELETE | `/api/playlists/{playlistId}/songs/{songId}` | Remove song from playlist |
| POST | `/api/playlists/{id}/save` | Save playlist to file |
| GET | `/api/playlists/{id}/download` | Download playlist as ZIP (audio files + M3U playlist) |

**Example - Create Playlist:**
```bash
curl -X POST http://localhost:8081/api/playlists \
  -H "Content-Type: application/json" \
  -d '{"name":"My Favorites","description":"A collection of my favorite songs"}'
```

**Example - Add Song to Playlist:**
```bash
curl -X POST http://localhost:8081/api/playlists/1/songs/1
```

### File Upload

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/upload/zip` | Upload ZIP file containing audio files. Supports `?force=true` for duplicate confirmation. |

#### Duplicate Album Detection & Confirmation Flow

Before processing a ZIP, the service automatically checks whether any album titles inside it already exist in the database (case-insensitive match on title only).

**Query Parameter:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `force` | boolean | `false` | When `false`, the endpoint first analyzes the ZIP. If duplicates are found it returns a warning instead of processing. Set to `true` to bypass the check and proceed anyway. |

**Behavior:**

1. **First call** (without `force` or `force=false`):
   - The server performs a dry-run analysis of all album titles in the ZIP.
   - If any album already exists → returns **HTTP 409 Conflict** with an `UploadAnalysisResponse` containing:
     - `existingAlbums`: list of album titles that would be duplicated
     - `newAlbums`: albums that would be created
     - `hasExistingAlbums`, `totalAlbumsInZip`, `message`

2. **Call with `?force=true`**:
   - Processing always occurs. Existing albums are reused; new songs are added to them.

**Example - Normal upload (no duplicates):**
```bash
curl -X POST http://localhost:8081/api/upload/zip \
  -F "file=@/path/to/music.zip"
```

**Example success response:**
```json
{
  "success": true,
  "message": "File uploaded and processed successfully",
  "data": {
    "success": true,
    "message": "Processed 10 audio files. Created: 2 artists, 3 albums, 10 songs. Skipped: 0 files",
    "extractionPath": "/tmp/music-extracted/20240429_211739_1234abcd"
  },
  "errors": null
}
```

**Example - Duplicates detected (409 response):**
```json
{
  "success": true,
  "message": "Some albums already exist. Set force=true for continue.",
  "data": {
    "hasExistingAlbums": true,
    "existingAlbums": ["The Dark Side of the Moon", "Wish You Were Here"],
    "newAlbums": ["Animals"],
    "totalAlbumsInZip": 3,
    "message": "Some albums already exist."
  },
  "errors": null
}
```

To force the upload despite duplicates:
```bash
curl -X POST "http://localhost:8081/api/upload/zip?force=true" \
  -F "file=@/path/to/music.zip"
```

**Bulk upload (all *.zip files in current directory):**
```bash
for X in *.zip; do  curl -X POST "http://localhost:8081/api/upload/zip" -F "file=@$X"; echo; done
```

**ZIP File Structure:**
The ZIP file should contain audio files (MP3, FLAC, OGG, WAV, M4A) organized in any directory structure. The service will:
1. Extract the ZIP to a persistent directory (configured via `app.upload.extracted-dir`, default: `/tmp/music-extracted`)
2. Scan for supported audio files recursively
3. Read ID3/metadata tags from each file
4. **Store the absolute file path** of each audio file in the `songs.filePath` field
5. Create artists, albums, and songs in the database (reusing existing albums when `force=true`)
6. Return a summary of processed files including the extraction path

**Configuration:**
```properties
# Maximum file size (default: 500MB)
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB

# Extracted files storage directory (default: /tmp/music-extracted)
app.upload.extracted-dir=/path/to/your/extracted-files

# Playlists storage directory (default: /tmp/playlists)
app.playlists.dir=/path/to/your/playlists
```

**Backup of extracted directories (music-extracted):**

The service extracts uploads into a nested structure:

```
/tmp/music-extracted/<timestamp>/
    <Album>/
      01 - Track.mp3
      02 - Track.mp3
      cover.jpg
      <scans>
```

To create per-album ZIP backups **omitting the first directory (timestamp)**, run this from inside the music-extracted folder:

```bash
for dir in */*/; do
  zipname="${dir#*/}"
  zipname="${zipname%/}.zip"
  parent="${dir%%/*}"
  subdir="${dir#*/}"
  subdir="${subdir%/}"
  (cd "$parent" && zip -r "../$zipname" "$subdir")
done
```

Result: `Album.zip` (containing `Album/...` but without the timestamp path).

**Required Tags:**
- `ARTIST` (artist name)
- `ALBUM` (album title)
- `TITLE` (song title)

Optional tags:
- `TRACK` (track number)
- `YEAR` (release year)
- `GENRE` (genere)
- Duration is read from audio header

**Note:** Duplicate artists (by name) are automatically detected and reused. Albums are matched by title only (case-insensitive). When duplicates are detected, the API returns a 409 warning so the client can ask the user for confirmation before proceeding with `?force=true`.

## Web Interface

The service includes a Thymeleaf-based web UI for managing your music collection.

### Web Pages

| URL | Description |
|-----|-------------|
| `/` | Dashboard with statistics and recent albums |
| `/albums` | Browse all albums with cover thumbnails |
| `/songs` | Browse all songs searchable by title/album/artist |
| `/playlists` | Browse all playlists and their songs |
| `/playlists/{id}` | View playlist details |
| `/upload` | ZIP file upload form with drag & drop |

### Features
- Responsive design (Bootstrap 5)
- Album grid view with cover images
- Album detail modals
- Song list with search and pagination
- Playlist creation and management
- Upload progress bar and drag-and-drop
- Real-time statistics

### Screenshots

**Dashboard:**
Shows total counts (artists, albums, songs, playlists, covers) and recently added albums with covers.

**Albums Page:**
Grid of album cards showing cover art, title, artist, year, genere, and song count.

**Songs Page:**
Table view with song details including file path, album, artist, with pagination.

**Playlists Page:**
List of user-created playlists with song counts and descriptions.

**Upload Page:**
Drag-and-drop ZIP uploader with progress indicator and status messages.

## Data Model

### Artist
- `id` (auto-generated)
- `name` (unique, required, 2-100 chars)
- `biography` (optional, max 1000 chars)

### Album
- `id` (auto-generated)
- `title` (required, 2-200 chars)
- `releaseYear` (optional, 1000-2100)
- `genere` (optional, max 50 chars)
- `coverImage` (binary, optional JPEG/PNG up to 5MB)
- `coverContentType` (MIME type, e.g., `image/jpeg`)
- `hasCover` (boolean, derived field in responses)
- `artist` (many-to-one relationship)

### Song
- `id` (auto-generated)
- `title` (required, 2-200 chars)
- `trackNumber` (optional, 1-999)
- `durationSeconds` (optional)
- `genere` (optional, max 50 chars)
- `filePath` (optional, absolute path to the audio file on server after extraction, max 500 chars)
- `originalArtist` (optional, max 100 chars)
- `album` (many-to-one relationship)

### Playlist
- `id` (auto-generated)
- `name` (required, 1-100 chars)
- `description` (optional, max 500 chars)
- `createdAt` (auto-generated timestamp)
- `updatedAt` (auto-updated timestamp)
- `songs` (many-to-many relationship)

## Security

- **SQL Injection Prevention**: All database queries use JPA parameter binding
- **Input Validation**: All inputs validated with Jakarta Bean Validation
- **Path Traversal Protection**: ZIP extraction sanitizes file paths
- **File Type Validation**: Only ZIP files accepted; only audio files processed

## Error Handling

Errors return appropriate HTTP status codes:

- `400 Bad Request` - Validation errors, invalid file
- `404 Not Found` - Resource not found
- `409 Conflict` - Duplicate artist name
- `500 Internal Server Error` - Unexpected errors

Example error response:
```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": [
    {"field": "name", "message": "Artist name is required"}
  ]
}
```

## Logging

Logs are configured at:
- `com.example.musicservice` - DEBUG level
- `org.springframework` - INFO level

Logs include operation details, errors, and warnings for missing tags or invalid files.

## Database Schema

The application uses Hibernate's `ddl-auto=update` to auto-create/update tables:

```sql
CREATE TABLE artists (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  biography VARCHAR(1000)
);

CREATE TABLE albums (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  release_year INTEGER,
  genere VARCHAR(50),
  cover_content_type VARCHAR(50),
  cover_image BLOB,
  artist_id BIGINT NOT NULL,
  FOREIGN KEY (artist_id) REFERENCES artists(id)
);

CREATE TABLE songs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  track_number INTEGER,
  duration_seconds INTEGER,
  genere VARCHAR(50),
  file_path VARCHAR(500),
  original_artist VARCHAR(100),
  album_id BIGINT NOT NULL,
  FOREIGN KEY (album_id) REFERENCES albums(id)
);

CREATE TABLE playlists (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE playlist_songs (
  playlist_id BIGINT NOT NULL,
  song_id BIGINT NOT NULL,
  PRIMARY KEY (playlist_id, song_id),
  FOREIGN KEY (playlist_id) REFERENCES playlists(id),
  FOREIGN KEY (song_id) REFERENCES songs(id)
);
```

## Testing

Run tests with:
```bash
mvn test
```

## Building

Create a fat JAR:
```bash
mvn clean package
```

Run the JAR:
```bash
java -jar target/music-service-1.0.0.jar
```

## Project Structure

```
music-service/
├── pom.xml
├── src/main/java/com/example/musicservice/
│   ├── MusicServiceApplication.java    # Main application class
│   ├── controller/                     # REST controllers
│   │   ├── AlbumController.java
│   │   ├── AlbumZipController.java     # ZIP download for albums + M3U
│   │   ├── ArtistController.java
│   │   ├── PlaylistController.java
│   │   ├── PlaylistZipController.java  # ZIP download for playlists + M3U
│   │   ├── SongController.java
│   │   ├── ThymeleafController.java    # MVC controller for Thymeleaf pages
│   │   └── UploadController.java
│   ├── dto/                            # Data transfer objects
│   │   ├── ApiResponse.java
│   │   ├── AlbumRequest.java
│   │   ├── AlbumResponse.java
│   │   ├── ArtistRequest.java
│   │   ├── ArtistResponse.java
│   │   ├── PaginatedResponse.java
│   │   ├── PlaylistRequest.java
│   │   ├── PlaylistResponse.java
│   │   ├── SongRequest.java
│   │   ├── SongResponse.java
│   │   ├── UploadAnalysisResponse.java
│   │   ├── UploadResponse.java
│   │   ├── web/
│   │   │   ├── AlbumViewDTO.java
│   │   │   ├── PlaylistViewDTO.java
│   │   │   └── SongViewDTO.java
│   ├── exception/                      # Exception handling
│   │   ├── GlobalExceptionHandler.java
│   │   ├── InvalidFileException.java
│   │   └── ResourceNotFoundException.java
│   ├── model/                          # JPA entities
│   │   ├── Album.java
│   │   ├── Artist.java
│   │   ├── Playlist.java
│   │   └── Song.java
│   ├── repository/                     # Spring Data JPA repositories
│   │   ├── AlbumRepository.java
│   │   ├── ArtistRepository.java
│   │   ├── PlaylistRepository.java
│   │   └── SongRepository.java
│   └── service/                        # Business logic
│       ├── AlbumService.java
│       ├── ArtistService.java
│       ├── PlaylistService.java
│       ├── SongService.java
│       └── ZipUploadService.java
├── src/main/resources/
│   ├── application.properties
│   ├── static/
│   │   ├── album-detail.js
│   │   ├── albums.js
│   │   ├── artist.js
│   │   ├── favicon.ico
│   │   ├── playlist-detail.js
│   │   ├── playlists.js
│   │   ├── songs.js
│   │   ├── styles.css
│   │   ├── theme.js
│   │   └── upload.js
│   └── templates/
│       ├── layout.html
│       ├── fragments/
│       │   ├── album-detail.html
│       │   ├── albums.html
│       │   ├── dashboard.html
│       │   ├── header.html
│       │   ├── playlist-detail.html
│       │   ├── playlists.html
│       │   ├── songs.html
│       │   └── upload.html
├── README.md
└── .gitignore
```

## Future Improvements

- Add authentication/authorization (Spring Security)
- Support additional metadata fields (composer, lyrics, etc.)
- Add file validation (actual MIME type checking)
- Add rate limiting for uploads
- Add integration tests
- Support for MySQL (PostgreSQL support documented above)
- Add caching (Redis)
- Add Swagger/OpenAPI documentation
- Add support for streaming large files
