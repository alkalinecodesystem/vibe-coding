package com.example.musicservice.controller;

import java.util.List;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.musicservice.dto.AlbumRequest;
import com.example.musicservice.dto.AlbumResponse;
import com.example.musicservice.dto.ApiResponse;
import com.example.musicservice.dto.PaginatedResponse;
import com.example.musicservice.service.AlbumService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

	
	private final AlbumService albumService;

	@PostMapping
	public ResponseEntity<ApiResponse<AlbumResponse>> createAlbum(@Valid @RequestBody AlbumRequest request) {
		AlbumResponse response = albumService.createAlbum(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Album created successfully", response));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<AlbumResponse>> getAlbumById(@PathVariable Long id) {
		AlbumResponse response = albumService.getAlbumById(id);
		return ResponseEntity.ok(ApiResponse.success(response));
	}


	@GetMapping
	public ResponseEntity<ApiResponse<PaginatedResponse<AlbumResponse>>> getAllAlbums(
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		PaginatedResponse<AlbumResponse> response = albumService.getAlbumsPaginated(page, size);
		return ResponseEntity.ok(ApiResponse.success(response));
	}


	@GetMapping("/search")
	public ResponseEntity<ApiResponse<List<AlbumResponse>>> searchAlbums(@RequestParam(required = false) String q,
			@RequestParam(required = false, defaultValue = "all") String type) {
		List<AlbumResponse> albums;

		if (q == null || q.trim().isEmpty()) {
			albums = null;
		} else {
			String query = q.trim();
			switch (type.toLowerCase()) {
			case "title":
				albums = albumService.searchAlbumsByTitle(query);
				break;
			case "artist":
				albums = albumService.searchAlbumsByArtist(query);
				break;
			case "song":
				albums = albumService.searchAlbumsBySong(query);
				break;
			case "songOriginalArtist":
				albums = albumService.searchAlbumsBySongOriginalArtist(query);
				break;
			case "songGenere":
				albums = albumService.searchAlbumsBySongGenere(query);
				break;
			case "all":
			default:
				albums = albumService.searchAlbumsAllFields(query);
				break;
			}
		}
		return ResponseEntity.ok(ApiResponse.success(albums));
	}

	@GetMapping("/search/title")
	public ResponseEntity<ApiResponse<List<AlbumResponse>>> searchAlbumsByTitle(@RequestParam String q) {
		List<AlbumResponse> albums = albumService.searchAlbumsByTitle(q);
		return ResponseEntity.ok(ApiResponse.success(albums));
	}

	@GetMapping("/search/artist")
	public ResponseEntity<ApiResponse<List<AlbumResponse>>> searchAlbumsByArtist(@RequestParam String q) {
		List<AlbumResponse> albums = albumService.searchAlbumsByArtist(q);
		return ResponseEntity.ok(ApiResponse.success(albums));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<AlbumResponse>> updateAlbum(@PathVariable Long id,
			@Valid @RequestBody AlbumRequest request) {
		AlbumResponse response = albumService.updateAlbum(id, request);
		return ResponseEntity.ok(ApiResponse.success("Album updated successfully", response));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Object>> deleteAlbum(@PathVariable Long id) {
		albumService.deleteAlbum(id);
		return ResponseEntity.ok(ApiResponse.success("Album deleted successfully"));
	}

	@DeleteMapping("/{id}/files")
	public ResponseEntity<ApiResponse<Object>> deleteAlbumFiles(@PathVariable Long id) {
		albumService.deleteAlbumFiles(id);
		return ResponseEntity.ok(ApiResponse.success("Album files deleted from disk successfully"));
	}

	@PatchMapping("/{id}/genere")
	public ResponseEntity<ApiResponse<AlbumResponse>> updateAlbumGenere(@PathVariable Long id,
			@RequestParam String genere) {
		AlbumResponse response = albumService.updateGenere(id, genere);
		return ResponseEntity.ok(ApiResponse.success("Album genere updated successfully", response));
	}

	@PostMapping("/{id}/cover")
	public ResponseEntity<ApiResponse<Object>> uploadAlbumCover(@PathVariable Long id,
			@RequestParam("file") MultipartFile file) {
		albumService.updateAlbumCover(id, file);
		return ResponseEntity.ok(ApiResponse.success("Album cover uploaded successfully"));
	}

	@GetMapping("/{id}/cover")
	public ResponseEntity<byte[]> getAlbumCover(@PathVariable Long id) {
		byte[] cover = albumService.getAlbumCover(id);
		String contentType = albumService.getAlbumCoverContentType(id);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(cover);
	}

	@DeleteMapping("/{id}/cover")
	public ResponseEntity<ApiResponse<Object>> deleteAlbumCover(@PathVariable Long id) {
		albumService.deleteAlbumCover(id);
		return ResponseEntity.ok(ApiResponse.success("Album cover deleted successfully"));
	}

	@GetMapping("/with-covers")
	public ResponseEntity<ApiResponse<Page<AlbumResponse>>> getAlbumsWithCoverPaginated(Pageable pageable) {
		Page<AlbumResponse> albums = albumService.getAlbumsWithCoverPaginated(pageable);
		return ResponseEntity.ok(ApiResponse.success(albums));
	}

	@GetMapping("/by-artist/{artistId}")
	public ResponseEntity<ApiResponse<PaginatedResponse<AlbumResponse>>> getAlbumsByArtist(@PathVariable Long artistId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size,
			@RequestParam(value = "artistName", required = false) String artistName) {
		PaginatedResponse<AlbumResponse> response = albumService.getAlbumsByArtistIdPaginated(artistId, artistName, page, size);
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
