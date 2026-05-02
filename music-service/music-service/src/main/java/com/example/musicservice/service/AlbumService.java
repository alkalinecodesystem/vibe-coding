package com.example.musicservice.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.musicservice.dto.AlbumRequest;
import com.example.musicservice.dto.AlbumResponse;
import com.example.musicservice.dto.web.AlbumViewDTO;
import com.example.musicservice.exception.ResourceNotFoundException;
import com.example.musicservice.model.Album;
import com.example.musicservice.model.Artist;
import com.example.musicservice.model.Playlist;
import com.example.musicservice.model.Song;
import com.example.musicservice.repository.AlbumRepository;
import com.example.musicservice.repository.ArtistRepository;
import com.example.musicservice.repository.PlaylistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AlbumService {

	private static final Logger logger = LoggerFactory.getLogger(AlbumService.class);

	private final AlbumRepository albumRepository;
	private final ArtistRepository artistRepository;
	private final PlaylistRepository playlistRepository;

	public AlbumResponse createAlbum(AlbumRequest request) {
		logger.info("Creating album: {} for artist ID: {}", request.getTitle(), request.getArtistId());

		// Check for duplicate title (case-insensitive)
		String normalizedTitle = request.getTitle().trim();
		albumRepository.findByTitleIgnoreCase(normalizedTitle).ifPresent(existing -> {
			throw new IllegalArgumentException(
					"Album with title '" + normalizedTitle + "' already exists (ID: " + existing.getId() + ")");
		});

		Artist artist = artistRepository.findById(request.getArtistId())
				.orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + request.getArtistId()));

		Album album = new Album();
		album.setTitle(normalizedTitle);
		album.setReleaseYear(request.getReleaseYear());
		album.setGenre(request.getGenre());
		album.setArtist(artist);

		Album saved = albumRepository.save(album);
		logger.info("Album created with ID: {}", saved.getId());

		return convertToResponse(saved);
	}

	@Transactional(readOnly = true)
	public AlbumResponse getAlbumById(Long id) {
		logger.debug("Fetching album with ID: {}", id);
		Album album = albumRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));
		AlbumResponse response = convertToResponse(album);
		// Set cover image for API response
		if (album.getCoverImage() != null) {
			response.setCoverImage(album.getCoverImage());
			response.setCoverContentType(album.getCoverContentType());
		}
		return response;
	}

	@Transactional(readOnly = true)
	public List<AlbumResponse> getAllAlbums() {
		logger.debug("Fetching all albums");
		return albumRepository.findAll().stream().map(this::convertToResponse).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<AlbumResponse> searchAlbumsByTitle(String title) {
		logger.debug("Searching albums with title: {}", title);
		return albumRepository.findByTitleContainingIgnoreCase(title).stream().map(this::convertToResponse)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<AlbumResponse> searchAlbumsByArtist(String artistName) {
		logger.debug("Searching albums by artist: {}", artistName);
		return albumRepository.findByArtist_NameContainingIgnoreCase(artistName).stream().map(this::convertToResponse)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<AlbumResponse> searchAlbumsBySong(String songTitle) {
		logger.debug("Searching albums by song title: {}", songTitle);
		return albumRepository.findBySongs_TitleContainingIgnoreCase(songTitle).stream().map(this::convertToResponse)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<AlbumResponse> searchAlbumsAllFields(String query) {
		logger.debug("Searching albums by all fields: {}", query);
		List<Album> byTitle = albumRepository.findByTitleContainingIgnoreCase(query);
		List<Album> byArtist = albumRepository.findByArtist_NameContainingIgnoreCase(query);
		List<Album> bySong = albumRepository.findBySongs_TitleContainingIgnoreCase(query);

		// Combine and remove duplicates
		java.util.Set<Long> seenIds = new java.util.HashSet<>();
		java.util.List<Album> combined = new java.util.ArrayList<>();

		java.util.List<Album> all = new java.util.ArrayList<>();
		all.addAll(byTitle);
		all.addAll(byArtist);
		all.addAll(bySong);

		for (Album album : all) {
			if (seenIds.add(album.getId())) {
				combined.add(album);
			}
		}

		return combined.stream().map(this::convertToResponse).collect(Collectors.toList());
	}

	// View-specific search methods (return AlbumViewDTO for Thymeleaf)
	@Transactional(readOnly = true)
	public List<AlbumViewDTO> searchAlbumsByTitleForView(String title) {
		logger.debug("Searching albums by title for view: {}", title);
		return albumRepository.findByTitleContainingIgnoreCase(title).stream().map(this::convertToViewDTO)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<AlbumViewDTO> searchAlbumsByArtistForView(String artistName) {
		logger.debug("Searching albums by artist for view: {}", artistName);
		return albumRepository.findByArtist_NameContainingIgnoreCase(artistName).stream().map(this::convertToViewDTO)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<AlbumViewDTO> searchAlbumsBySongForView(String songTitle) {
		logger.debug("Searching albums by song title for view: {}", songTitle);
		return albumRepository.findBySongs_TitleContainingIgnoreCase(songTitle).stream().map(this::convertToViewDTO)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<AlbumViewDTO> searchAlbumsAllFieldsForView(String query) {
		logger.debug("Searching albums by all fields for view: {}", query);
		List<Album> byTitle = albumRepository.findByTitleContainingIgnoreCase(query);
		List<Album> byArtist = albumRepository.findByArtist_NameContainingIgnoreCase(query);
		List<Album> bySong = albumRepository.findBySongs_TitleContainingIgnoreCase(query);

		// Combine and remove duplicates
		java.util.Set<Long> seenIds = new java.util.HashSet<>();
		java.util.List<Album> combined = new java.util.ArrayList<>();

		java.util.List<Album> all = new java.util.ArrayList<>();
		all.addAll(byTitle);
		all.addAll(byArtist);
		all.addAll(bySong);

		for (Album album : all) {
			if (seenIds.add(album.getId())) {
				combined.add(album);
			}
		}

		return combined.stream().map(this::convertToViewDTO).collect(Collectors.toList());
	}

	public AlbumResponse updateAlbum(Long id, AlbumRequest request) {
		logger.info("Updating album ID: {}", id);

		Album album = albumRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));

		String normalizedTitle = request.getTitle().trim();
		// Check for duplicate title (exclude current album)
		albumRepository.findByTitleIgnoreCase(normalizedTitle).ifPresent(existing -> {
			if (!existing.getId().equals(id)) {
				throw new IllegalArgumentException(
						"Album with title '" + normalizedTitle + "' already exists (ID: " + existing.getId() + ")");
			}
		});

		Artist artist = artistRepository.findById(request.getArtistId())
				.orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + request.getArtistId()));

		album.setTitle(normalizedTitle);
		album.setReleaseYear(request.getReleaseYear());
		album.setGenre(request.getGenre());
		album.setArtist(artist);

		Album saved = albumRepository.save(album);
		logger.info("Album updated: {}", saved.getId());

		return convertToResponse(saved);
	}

	public void deleteAlbum(Long id) {
		logger.info("Deleting album ID: {}", id);

		Album album = albumRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));

		// First, remove all songs from playlists to avoid foreign key constraint
		// violations
		List<Playlist> allPlaylists = playlistRepository.findAll();
		for (Playlist playlist : allPlaylists) {
			boolean playlistModified = false;
			for (Song song : album.getSongs()) {
				if (playlist.getSongs().contains(song)) {
					playlist.removeSong(song);
					playlistModified = true;
					logger.info("Removed song {} from playlist {}", song.getId(), playlist.getId());
				}
			}
			if (playlistModified) {
				playlistRepository.save(playlist);
			}
		}

		albumRepository.delete(album);
		logger.info("Album deleted: {}", id);
	}

	public void deleteAlbumFiles(Long id) {
		logger.info("Deleting directory for album ID: {}", id);

		Album album = albumRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));

		if (!album.getSongs().isEmpty()) {
			String firstFilePath = album.getSongs().get(0).getFilePath();
			if (firstFilePath != null) {
				Path dirPath = Paths.get(firstFilePath).getParent();
				if (dirPath != null && Files.exists(dirPath)) {
					try {
						// Delete the directory and all its contents
						deleteDirectory(dirPath);
						logger.info("Deleted directory: {}", dirPath);
					} catch (IOException e) {
						logger.error("Failed to delete directory: {}", dirPath, e);
					}
				}
			}
		}
		logger.info("Finished deleting directory for album ID: {}", id);
	}

	private void deleteDirectory(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			// Delete all files and subdirectories first
			Files.walk(path).sorted((a, b) -> b.compareTo(a)) // Reverse order to delete files before directories
					.forEach(p -> {
						try {
							Files.delete(p);
						} catch (IOException e) {
							logger.error("Failed to delete: {}", p, e);
						}
					});
		} else {
			Files.delete(path);
		}
	}

	@Transactional(readOnly = true)
	public Page<AlbumResponse> getAlbumsWithCoverPaginated(Pageable pageable) {
		logger.debug("Fetching paginated albums with cover");
		return albumRepository.findByCoverImageIsNotNull(pageable).map(this::convertToResponse);
	}

	public AlbumResponse updateGenre(Long id, String genre) {
		if (genre != null && genre.length() > 50) {
			throw new IllegalArgumentException("Genre cannot exceed 50 characters");
		}

		Album album = albumRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));

		album.setGenre(genre != null ? genre.trim() : null);
		Album saved = albumRepository.save(album);
		return convertToResponse(saved);
	}

	@Transactional
	public void updateAlbumCover(Long id, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Cover image file is required");
		}

		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new IllegalArgumentException("Only image files are allowed for album cover");
		}

		// Limit size (e.g., 5MB max)
		if (file.getSize() > 5 * 1024 * 1024) {
			throw new IllegalArgumentException("Cover image size cannot exceed 5MB");
		}

		Album album = albumRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));

		try {
			album.setCoverImage(file.getBytes());
			album.setCoverContentType(contentType);
			albumRepository.save(album);
			logger.info("Album cover updated for album ID: {}", id);
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to read cover image", e);
		}
	}

	@Transactional(readOnly = true)
	public byte[] getAlbumCover(Long id) {
		Album album = albumRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));

		if (album.getCoverImage() == null) {
			throw new ResourceNotFoundException("Album cover not found for id: " + id);
		}
		return album.getCoverImage();
	}

	@Transactional(readOnly = true)
	public String getAlbumCoverContentType(Long id) {
		Album album = albumRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));

		if (album.getCoverContentType() == null) {
			throw new ResourceNotFoundException("Album cover not found for id: " + id);
		}
		return album.getCoverContentType();
	}

	@Transactional
	public void deleteAlbumCover(Long id) {
		Album album = albumRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));

		if (album.getCoverImage() != null) {
			album.setCoverImage(null);
			album.setCoverContentType(null);
			albumRepository.save(album);
			logger.info("Album cover deleted for album ID: {}", id);
		}
	}

	private AlbumResponse convertToResponse(Album album) {
		AlbumResponse response = new AlbumResponse();
		response.setId(album.getId());
		response.setTitle(album.getTitle());
		response.setReleaseYear(album.getReleaseYear());
		response.setGenre(album.getGenre());

		AlbumResponse.ArtistSummary artistSummary = new AlbumResponse.ArtistSummary();
		artistSummary.setId(album.getArtist().getId());
		artistSummary.setName(album.getArtist().getName());
		response.setArtist(artistSummary);

		List<AlbumResponse.SongSummary> songSummaries = album.getSongs().stream().map(song -> {
			AlbumResponse.SongSummary summary = new AlbumResponse.SongSummary();
			summary.setId(song.getId());
			summary.setTitle(song.getTitle());
			summary.setTrackNumber(song.getTrackNumber());
			summary.setDurationSeconds(song.getDurationSeconds());
			summary.setGenre(song.getGenre());
			summary.setFilePath(song.getFilePath());
			summary.setOriginalArtist(song.getOriginalArtist());
			summary.setFormattedDuration(song.getFormattedDuration());
			return summary;
		}).collect(Collectors.toList());

		response.setSongs(songSummaries);
		response.setHasCover(album.getCoverImage() != null);
		return response;
	}

	@Transactional(readOnly = true)
	public List<AlbumViewDTO> getAllAlbumsForView() {
		List<Album> albums = albumRepository.findAll();
		// Remove duplicates by album title only (keep most recent for each title)
		java.util.Map<String, Album> uniqueMap = new java.util.LinkedHashMap<>();
		albums.stream().sorted((a, b) -> Long.compare(b.getId(), a.getId())).forEach(album -> {
			String key = album.getTitle().toLowerCase().trim();
			uniqueMap.putIfAbsent(key, album);
		});
		return uniqueMap.values().stream().map(this::convertToViewDTO).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<AlbumViewDTO> getRecentAlbumsForView(int limit) {
		List<Album> albums = albumRepository.findAll();
		java.util.Map<String, Album> uniqueMap = new java.util.LinkedHashMap<>();
		albums.stream().sorted((a, b) -> Long.compare(b.getId(), a.getId())).forEach(album -> {
			String key = album.getTitle().toLowerCase().trim();
			uniqueMap.putIfAbsent(key, album);
		});
		return uniqueMap.values().stream().limit(limit).map(this::convertToViewDTO).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public AlbumViewDTO getAlbumViewById(Long id) {
		Album album = albumRepository.findByIdWithSongs(id)
				.orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));
		return convertToViewDTO(album);
	}

	private AlbumViewDTO convertToViewDTO(Album album) {
		AlbumViewDTO dto = new AlbumViewDTO();
		dto.setId(album.getId());
		dto.setTitle(album.getTitle());
		dto.setReleaseYear(album.getReleaseYear());
		dto.setGenre(album.getGenre());
		dto.setHasCover(album.getCoverImage() != null);
		if (album.getCoverImage() != null) {
			dto.setCoverContentType(album.getCoverContentType());
			dto.setCoverImageBase64(Base64.getEncoder().encodeToString(album.getCoverImage()));
		}
		dto.setArtistName(album.getArtist() != null ? album.getArtist().getName() : "Unknown");
		dto.setArtistId(album.getArtist() != null ? album.getArtist().getId() : null);
		dto.setSongCount(album.getSongs() != null ? album.getSongs().size() : 0);

		// Convert songs list, sorted by track number (nulls last)
		if (album.getSongs() != null) {
			List<com.example.musicservice.dto.web.SongViewDTO> songDTOs = album.getSongs().stream().sorted((s1, s2) -> {
				Integer t1 = s1.getTrackNumber();
				Integer t2 = s2.getTrackNumber();
				if (t1 == null && t2 == null)
					return 0;
				if (t1 == null)
					return 1; // nulls last
				if (t2 == null)
					return -1; // t1 before t2
				return t1.compareTo(t2);
			}).map(song -> {
				com.example.musicservice.dto.web.SongViewDTO sDTO = new com.example.musicservice.dto.web.SongViewDTO();
				sDTO.setId(song.getId());
				sDTO.setTitle(song.getTitle());
				sDTO.setTrackNumber(song.getTrackNumber());
				sDTO.setDurationSeconds(song.getDurationSeconds());
				sDTO.setGenre(song.getGenre());
				sDTO.setFilePath(song.getFilePath());
				sDTO.setOriginalArtist(song.getOriginalArtist());
				// Don't set album to avoid recursion
				return sDTO;
			}).collect(Collectors.toList());
			dto.setSongs(songDTOs);
		} else {
			dto.setSongs(java.util.Collections.emptyList());
		}

		return dto;
	}
}
