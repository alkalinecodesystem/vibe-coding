package com.example.musicservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import com.example.musicservice.dto.ArtistRequest;
import com.example.musicservice.dto.ArtistResponse;
import com.example.musicservice.dto.PaginatedResponse;
import com.example.musicservice.service.ArtistService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {

	private final ArtistService artistService;

	@PostMapping
	public ResponseEntity<ApiResponse<ArtistResponse>> createArtist(@Valid @RequestBody ArtistRequest request) {
		ArtistResponse response = artistService.createArtist(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Artist created successfully", response));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<ArtistResponse>> getArtistById(@PathVariable Long id) {
		ArtistResponse response = artistService.getArtistById(id);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<PaginatedResponse<ArtistResponse>>> getAllArtist(
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		PaginatedResponse<ArtistResponse> response = artistService.getArtistPaginated(page, size);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@GetMapping("/search")
	public ResponseEntity<ApiResponse<List<ArtistResponse>>> searchArtists(@RequestParam String q) {
		List<ArtistResponse> artists = artistService.searchArtists(q);
		return ResponseEntity.ok(ApiResponse.success(artists));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<ArtistResponse>> updateArtist(@PathVariable Long id,
			@Valid @RequestBody ArtistRequest request) {
		ArtistResponse response = artistService.updateArtist(id, request);
		return ResponseEntity.ok(ApiResponse.success("Artist updated successfully", response));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Object>> deleteArtist(@PathVariable Long id) {
		artistService.deleteArtist(id);
		return ResponseEntity.ok(ApiResponse.success("Artist deleted successfully"));
	}
}
