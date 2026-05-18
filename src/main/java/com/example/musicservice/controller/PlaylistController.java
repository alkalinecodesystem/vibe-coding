package com.example.musicservice.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.musicservice.dto.ApiResponse;
import com.example.musicservice.dto.PaginatedResponse;
import com.example.musicservice.dto.PlaylistRequest;
import com.example.musicservice.dto.PlaylistResponse;
import com.example.musicservice.service.PlaylistService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private static final Logger logger = LoggerFactory.getLogger(PlaylistController.class);

    private final PlaylistService playlistService;

    @PostMapping
    public ResponseEntity<ApiResponse<PlaylistResponse>> createPlaylist(@Valid @RequestBody PlaylistRequest request) {
        logger.info("Creating playlist: {}", request.getName());
        PlaylistResponse response = playlistService.createPlaylist(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Playlist created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaylistResponse>> getPlaylistById(@PathVariable Long id) {
        logger.debug("Fetching playlist with ID: {}", id);
        PlaylistResponse response = playlistService.getPlaylistById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<PlaylistResponse>>> getAllPlaylists(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {
        logger.debug("Fetching playlists paginated: page={}, size={}", page, size);
        PaginatedResponse<PlaylistResponse> response = playlistService.getPlaylistsPaginated(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaylistResponse>> updatePlaylist(@PathVariable Long id,
            @Valid @RequestBody PlaylistRequest request) {
        logger.info("Updating playlist ID: {}", id);
        PlaylistResponse response = playlistService.updatePlaylist(id, request);
        return ResponseEntity.ok(ApiResponse.success("Playlist updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deletePlaylist(@PathVariable Long id) {
        logger.info("Deleting playlist ID: {}", id);
        playlistService.deletePlaylist(id);
        return ResponseEntity.ok(ApiResponse.success("Playlist deleted successfully"));
    }

    @PostMapping("/{playlistId}/songs/{songId}")
    public ResponseEntity<ApiResponse<PlaylistResponse>> addSongToPlaylist(@PathVariable Long playlistId,
            @PathVariable Long songId) {
        logger.info("Adding song {} to playlist {}", songId, playlistId);
        PlaylistResponse response = playlistService.addSongToPlaylist(playlistId, songId);
        return ResponseEntity.ok(ApiResponse.success("Song added to playlist successfully", response));
    }

    @DeleteMapping("/{playlistId}/songs/{songId}")
    public ResponseEntity<ApiResponse<PlaylistResponse>> removeSongFromPlaylist(@PathVariable Long playlistId,
            @PathVariable Long songId) {
        logger.info("Removing song {} from playlist {}", songId, playlistId);
        PlaylistResponse response = playlistService.removeSongFromPlaylist(playlistId, songId);
        return ResponseEntity.ok(ApiResponse.success("Song removed from playlist successfully", response));
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<ApiResponse<Object>> savePlaylistToFile(@PathVariable Long id) {
        logger.info("Saving playlist {} to file", id);
        try {
            playlistService.savePlaylistToFile(id);
            return ResponseEntity.ok(ApiResponse.success("Playlist saved to file successfully"));
        } catch (IOException e) {
            logger.error("Failed to save playlist {} to file", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to save playlist to file: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/download")
    public void downloadPlaylist(@PathVariable Long id, HttpServletResponse response) throws IOException {
        logger.info("Downloading playlist {} as zip", id);
        com.example.musicservice.dto.PlaylistResponse playlist = playlistService.getPlaylistById(id);
        List<Path> tracks = playlistService.getFilesForPlaylist(id);

        // Build M3U with playlist metadata
        StringBuilder m3u = new StringBuilder();
        m3u.append("#EXTM3U\n");
        m3u.append("#PLAYLIST:").append(playlist.getName()).append("\n");
        if (playlist.getDescription() != null && !playlist.getDescription().isEmpty()) {
            m3u.append("#DESCRIPTION:").append(playlist.getDescription()).append("\n");
        }
        m3u.append("#CREATED:").append(playlist.getCreatedAt()).append("\n");
		for (com.example.musicservice.dto.PlaylistResponse.SongSummary s : playlist.getSongs()) {
				String artist = s.getOriginalArtist();
				String displayInfo = (artist != null ? artist + " - " : "") + s.getTitle();
				m3u.append("#EXTINF:-1,").append(displayInfo).append("\n");
				String fp = s.getFilePath();
				String fileName = (fp != null && !fp.isEmpty()) ? java.nio.file.Paths.get(fp).getFileName().toString() : "";
				m3u.append(fileName).append("\n");
			}
        byte[] m3uBytes = m3u.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"playlist-" + playlist.getName() + ".zip\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            byte[] buffer = new byte[8192];
            Map<String, Integer> nameCounts = new HashMap<>();
            for (Path track : tracks) {
                if (track == null || !Files.exists(track) || !Files.isRegularFile(track)) continue;
                String originalName = track.getFileName().toString();
                int count = nameCounts.getOrDefault(originalName, 0);
                String entryName = originalName;
                if (count > 0) {
                    int dot = originalName.lastIndexOf('.');
                    String base = dot > 0 ? originalName.substring(0, dot) : originalName;
                    String ext = dot > 0 ? originalName.substring(dot) : "";
                    entryName = base + "_" + count + ext;
                }
                nameCounts.put(originalName, count + 1);
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                try (InputStream in = Files.newInputStream(track)) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
            ZipEntry m3uEntry = new ZipEntry("playlist-" + playlist.getName() + ".m3u");
            zos.putNextEntry(m3uEntry);
            zos.write(m3uBytes);
            zos.closeEntry();
            zos.finish();
        } catch (IOException e) {
            logger.error("Error streaming zip for playlist {}", id, e);
            response.reset();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
 }
