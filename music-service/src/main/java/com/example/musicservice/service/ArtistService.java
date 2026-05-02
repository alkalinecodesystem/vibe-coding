package com.example.musicservice.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.musicservice.dto.ArtistRequest;
import com.example.musicservice.dto.ArtistResponse;
import com.example.musicservice.exception.ResourceNotFoundException;
import com.example.musicservice.model.Artist;
import com.example.musicservice.repository.ArtistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtistService {

	private static final Logger logger = LoggerFactory.getLogger(ArtistService.class);

	private final ArtistRepository artistRepository;

	public ArtistResponse createArtist(ArtistRequest request) {
		logger.info("Creating artist: {}", request.getName());

		if (artistRepository.existsByNameIgnoreCase(request.getName())) {
			throw new IllegalArgumentException("Artist with name '" + request.getName() + "' already exists");
		}

		Artist artist = new Artist();
		artist.setName(request.getName());
		artist.setBiography(request.getBiography());

		Artist saved = artistRepository.save(artist);
		logger.info("Artist created with ID: {}", saved.getId());

		return convertToResponse(saved);
	}

	@Transactional(readOnly = true)
	public ArtistResponse getArtistById(Long id) {
		logger.debug("Fetching artist with ID: {}", id);
		Artist artist = artistRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + id));
		return convertToResponse(artist);
	}

	@Transactional(readOnly = true)
	public List<ArtistResponse> getAllArtists() {
		logger.debug("Fetching all artists");
		return artistRepository.findAll().stream().map(this::convertToResponse).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<ArtistResponse> searchArtists(String query) {
		logger.debug("Searching artists with query: {}", query);
		return artistRepository.findByNameContainingIgnoreCase(query).stream().map(this::convertToResponse)
				.collect(Collectors.toList());
	}

	public ArtistResponse updateArtist(Long id, ArtistRequest request) {
		logger.info("Updating artist ID: {}", id);

		Artist artist = artistRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + id));

		if (!artist.getName().equalsIgnoreCase(request.getName())
				&& artistRepository.existsByNameIgnoreCase(request.getName())) {
			throw new IllegalArgumentException("Artist with name '" + request.getName() + "' already exists");
		}

		artist.setName(request.getName());
		artist.setBiography(request.getBiography());

		Artist saved = artistRepository.save(artist);
		logger.info("Artist updated: {}", saved.getId());

		return convertToResponse(saved);
	}

	public void deleteArtist(Long id) {
		logger.info("Deleting artist ID: {}", id);

		Artist artist = artistRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + id));

		// Check if artist has albums
		if (!artist.getAlbums().isEmpty()) {
			throw new IllegalStateException("Cannot delete artist with associated albums. Remove albums first.");
		}

		artistRepository.delete(artist);
		logger.info("Artist deleted: {}", id);
	}

	private ArtistResponse convertToResponse(Artist artist) {
		ArtistResponse response = new ArtistResponse();
		response.setId(artist.getId());
		response.setName(artist.getName());
		response.setBiography(artist.getBiography());

		List<ArtistResponse.AlbumSummary> albumSummaries = artist.getAlbums().stream().map(album -> {
			ArtistResponse.AlbumSummary summary = new ArtistResponse.AlbumSummary();
			summary.setId(album.getId());
			summary.setTitle(album.getTitle());
			summary.setReleaseYear(album.getReleaseYear());
			return summary;
		}).collect(Collectors.toList());

		response.setAlbums(albumSummaries);
		return response;
	}
}
