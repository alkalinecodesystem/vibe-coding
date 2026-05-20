package com.example.musicservice.controller;

import java.io.IOException;

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

 }
