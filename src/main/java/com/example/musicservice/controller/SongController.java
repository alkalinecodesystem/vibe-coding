package com.example.musicservice.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.musicservice.dto.ApiResponse;
import com.example.musicservice.dto.PaginatedResponse;
import com.example.musicservice.dto.SongRequest;
import com.example.musicservice.dto.SongResponse;
import com.example.musicservice.service.SongService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {

	private final SongService songService;

	@Value("${app.upload.extracted-dir}")
	private String extractedDir; // ❗ not final

	@PostMapping
	public ResponseEntity<ApiResponse<SongResponse>> createSong(@Valid @RequestBody SongRequest request) {
		SongResponse response = songService.createSong(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Song created successfully", response));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<SongResponse>> getSongById(@PathVariable Long id) {
		SongResponse response = songService.getSongById(id);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<PaginatedResponse<SongResponse>>> getAllSongs(
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String q, @RequestParam(defaultValue = "all") String type) {
		PaginatedResponse<SongResponse> response = songService.getSongsPaginated(page, size, q, type);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@GetMapping("/search/title")
	public ResponseEntity<ApiResponse<List<SongResponse>>> searchSongsByTitle(@RequestParam String q) {
		List<SongResponse> songs = songService.searchSongsByTitle(q);
		return ResponseEntity.ok(ApiResponse.success(songs));
	}

	@GetMapping("/search/album")
	public ResponseEntity<ApiResponse<List<SongResponse>>> searchSongsByAlbum(@RequestParam String q) {
		List<SongResponse> songs = songService.searchSongsByAlbum(q);
		return ResponseEntity.ok(ApiResponse.success(songs));
	}

	@GetMapping("/search/artist")
	public ResponseEntity<ApiResponse<List<SongResponse>>> searchSongsByArtist(@RequestParam String q) {
		List<SongResponse> songs = songService.searchSongsByArtist(q);
		return ResponseEntity.ok(ApiResponse.success(songs));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<SongResponse>> updateSong(@PathVariable Long id,
			@Valid @RequestBody SongRequest request) {
		SongResponse response = songService.updateSong(id, request);
		return ResponseEntity.ok(ApiResponse.success("Song updated successfully", response));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Object>> deleteSong(@PathVariable Long id) {
		songService.deleteSong(id);
		return ResponseEntity.ok(ApiResponse.success("Song deleted successfully"));
	}

	@GetMapping("/file")
	public ResponseEntity<Resource> getSongFile(@RequestParam String path) {
		try {
			// Decode the path parameter (it might be URL encoded)
			String decodedPath = java.net.URLDecoder.decode(path, "UTF-8");

			// Security: Ensure the path is within the allowed directory
			// We'll check if it starts with the configured extracted directory
			String baseDir = System.getProperty("app.upload.extracted-dir", "/tmp/music-extracted");
			Path filePath = Paths.get(decodedPath);

			// Optional: Add additional security checks to prevent directory traversal
			Path absolutePath = filePath.toAbsolutePath().normalize();
			Path baseDirectory = Paths.get(baseDir).toAbsolutePath().normalize();

			// Check if the file is within the base directory
			if (!absolutePath.startsWith(baseDirectory)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
			}

			// Check if file exists
			if (!Files.exists(absolutePath)) {
				return ResponseEntity.notFound().build();
			}

			// Determine content type based on file extension
			String contentType = "application/octet-stream"; // default
			String fileName = absolutePath.getFileName().toString().toLowerCase();
			if (fileName.endsWith(".mp3")) {
				contentType = "audio/mpeg";
			} else if (fileName.endsWith(".flac")) {
				contentType = "audio/flac";
			} else if (fileName.endsWith(".wav")) {
				contentType = "audio/wav";
			} else if (fileName.endsWith(".m4a") || fileName.endsWith(".aac")) {
				contentType = "audio/mp4";
			} else if (fileName.endsWith(".ogg")) {
				contentType = "audio/ogg";
			}

			Resource resource = new FileSystemResource(absolutePath.toFile());

			return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
					.body(resource);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		} catch (IllegalArgumentException e) {
			// Handle URL decoding errors
			return ResponseEntity.badRequest().build();
		}
	}
}