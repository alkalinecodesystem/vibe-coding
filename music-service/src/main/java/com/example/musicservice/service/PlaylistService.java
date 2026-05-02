package com.example.musicservice.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.musicservice.dto.PlaylistRequest;
import com.example.musicservice.dto.PlaylistResponse;
import com.example.musicservice.dto.web.PlaylistViewDTO;
import com.example.musicservice.exception.ResourceNotFoundException;
import com.example.musicservice.model.Playlist;
import com.example.musicservice.model.Song;
import com.example.musicservice.repository.PlaylistRepository;
import com.example.musicservice.repository.SongRepository;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaylistService {

	private static final Logger logger = LoggerFactory.getLogger(PlaylistService.class);

	private final PlaylistRepository playlistRepository;
	private final SongRepository songRepository;

	@Value("${app.playlists.dir:/home/user/playlists}")
	private String playlistsDir;

	public PlaylistResponse createPlaylist(PlaylistRequest request) {
		logger.info("Creating playlist: {}", request.getName());

		Playlist playlist = new Playlist();
		playlist.setName(request.getName());
		playlist.setDescription(request.getDescription());

		Playlist saved = playlistRepository.save(playlist);
		logger.info("Playlist created with ID: {}", saved.getId());

		return convertToResponse(saved);
	}

	@Transactional(readOnly = true)
	public PlaylistResponse getPlaylistById(Long id) {
		logger.debug("Fetching playlist with ID: {}", id);
		Playlist playlist = playlistRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Playlist not found with id: " + id));
		return convertToResponse(playlist);
	}

	@Transactional(readOnly = true)
	public List<PlaylistResponse> getAllPlaylists() {
		logger.debug("Fetching all playlists");
		return playlistRepository.findAllByOrderByCreatedAtDesc().stream().map(this::convertToResponse)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<PlaylistViewDTO> getAllPlaylistsForView() {
		logger.debug("Fetching all playlists for view");
		return playlistRepository.findAllByOrderByCreatedAtDesc().stream().map(this::convertToViewDTO)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<PlaylistResponse> searchPlaylistsByName(String name) {
		logger.debug("Searching playlists with name: {}", name);
		return playlistRepository.findByNameContainingIgnoreCase(name).stream().map(this::convertToResponse)
				.collect(Collectors.toList());
	}

	public PlaylistResponse updatePlaylist(Long id, PlaylistRequest request) {
		logger.info("Updating playlist ID: {}", id);

		Playlist playlist = playlistRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Playlist not found with id: " + id));

		playlist.setName(request.getName());
		playlist.setDescription(request.getDescription());

		Playlist saved = playlistRepository.save(playlist);
		return convertToResponse(saved);
	}

	public void deletePlaylist(Long id) {
		logger.info("Deleting playlist ID: {}", id);
		Playlist playlist = playlistRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Playlist not found with id: " + id));

		// Delete file from filesystem if it exists
		deletePlaylistFile(id);

		playlistRepository.delete(playlist);
	}

	public PlaylistResponse addSongToPlaylist(Long playlistId, Long songId) {
		logger.info("Adding song {} to playlist {}", songId, playlistId);

		Playlist playlist = playlistRepository.findById(playlistId)
				.orElseThrow(() -> new ResourceNotFoundException("Playlist not found with id: " + playlistId));

		Song song = songRepository.findById(songId)
				.orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + songId));

		if (!playlist.hasSong(song)) {
			playlist.addSong(song);
			playlistRepository.save(playlist);
		}

		return convertToResponse(playlist);
	}

	public PlaylistResponse removeSongFromPlaylist(Long playlistId, Long songId) {
		logger.info("Removing song {} from playlist {}", songId, playlistId);

		Playlist playlist = playlistRepository.findById(playlistId)
				.orElseThrow(() -> new ResourceNotFoundException("Playlist not found with id: " + playlistId));

		Song song = songRepository.findById(songId)
				.orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + songId));

		playlist.removeSong(song);
		playlistRepository.save(playlist);

		return convertToResponse(playlist);
	}

	public void savePlaylistToFile(Long playlistId) throws IOException {
		logger.info("Saving playlist {} to file", playlistId);

		Playlist playlist = playlistRepository.findById(playlistId)
				.orElseThrow(() -> new ResourceNotFoundException("Playlist not found with id: " + playlistId));

		// Create playlists directory if it doesn't exist
		Path playlistsPath = Paths.get(playlistsDir);
		Files.createDirectories(playlistsPath);

		// Create filename from playlist name (sanitize)
		String filename = playlist.getName().replaceAll("[^a-zA-Z0-9\\s-]", "").replaceAll("\\s+", "_") + ".m3u";
		Path filePath = playlistsPath.resolve(filename);

		// Write M3U playlist file
		try (var writer = Files.newBufferedWriter(filePath)) {
			// Write M3U header
			writer.write("#EXTM3U");
			writer.newLine();

			// Write playlist information as comment
			writer.write("#PLAYLIST:" + playlist.getName());
			writer.newLine();
			if (playlist.getDescription() != null && !playlist.getDescription().isEmpty()) {
				writer.write("#DESCRIPTION:" + playlist.getDescription());
				writer.newLine();
			}
			writer.write("#CREATED:" + playlist.getCreatedAt());
			writer.newLine();

			// Write each song
			for (Song song : playlist.getSongs()) {
				// EXTINF line with duration and title/artist info
				int duration = song.getDurationSeconds() != null ? song.getDurationSeconds() : -1;
				String artist = song.getOriginalArtist();
				if (artist == null && song.getAlbum() != null && song.getAlbum().getArtist() != null) {
					artist = song.getAlbum().getArtist().getName();
				}
				String title = song.getTitle();
				String displayInfo = (artist != null ? artist + " - " : "") + title;

				writer.write("#EXTINF:" + duration + "," + displayInfo);
				writer.newLine();

				// File path
				writer.write(song.getFilePath());
				writer.newLine();
			}
		}

		logger.info("Playlist saved to M3U file: {}", filePath);
	}

	private void deletePlaylistFile(Long playlistId) {
		try {
			Playlist playlist = playlistRepository.findById(playlistId).orElse(null);
			if (playlist != null) {
				String filename = playlist.getName().replaceAll("[^a-zA-Z0-9\\s-]", "").replaceAll("\\s+", "_")
						+ ".m3u";
				Path filePath = Paths.get(playlistsDir, filename);
				Files.deleteIfExists(filePath);
				logger.info("Deleted playlist file: {}", filePath);
			}
		} catch (IOException e) {
			logger.warn("Failed to delete playlist file for ID {}: {}", playlistId, e.getMessage());
		}
	}

	private PlaylistResponse convertToResponse(Playlist playlist) {
		PlaylistResponse response = new PlaylistResponse();
		response.setId(playlist.getId());
		response.setName(playlist.getName());
		response.setDescription(playlist.getDescription());
		response.setCreatedAt(playlist.getCreatedAt());
		response.setUpdatedAt(playlist.getUpdatedAt());
		response.setSongCount(playlist.getSongs().size());

		List<PlaylistResponse.SongSummary> songSummaries = playlist.getSongs().stream().map(this::convertSongToSummary)
				.collect(Collectors.toList());
		response.setSongs(songSummaries);

		// Set cover image from the first song's album that has a cover
		for (Song song : playlist.getSongs()) {
			if (song.getAlbum() != null && song.getAlbum().getCoverImage() != null) {
				response.setCoverImage(song.getAlbum().getCoverImage());
				response.setCoverContentType(song.getAlbum().getCoverContentType());
				break; // Use the first available cover
			}
		}

		return response;
	}

	private PlaylistResponse.SongSummary convertSongToSummary(Song song) {
		PlaylistResponse.SongSummary summary = new PlaylistResponse.SongSummary();
		summary.setId(song.getId());
		summary.setTitle(song.getTitle());
		summary.setTrackNumber(song.getTrackNumber());
		summary.setFormattedDuration(song.getFormattedDuration());
		summary.setGenre(song.getGenre());
		summary.setOriginalArtist(song.getOriginalArtist());
		summary.setFilePath(song.getFilePath());

		if (song.getAlbum() != null) {
			PlaylistResponse.SongSummary.AlbumSummary albumSummary = new PlaylistResponse.SongSummary.AlbumSummary();
			albumSummary.setId(song.getAlbum().getId());
			albumSummary.setTitle(song.getAlbum().getTitle());
			if (song.getAlbum().getArtist() != null) {
				PlaylistResponse.SongSummary.AlbumSummary.ArtistSummary artistSummary = new PlaylistResponse.SongSummary.AlbumSummary.ArtistSummary();
				artistSummary.setId(song.getAlbum().getArtist().getId());
				artistSummary.setName(song.getAlbum().getArtist().getName());
				albumSummary.setArtist(artistSummary);
			}
			summary.setAlbum(albumSummary);
		}

		return summary;
	}

	private PlaylistViewDTO convertToViewDTO(Playlist playlist) {
		PlaylistViewDTO dto = new PlaylistViewDTO();
		dto.setId(playlist.getId());
		dto.setName(playlist.getName());
		dto.setDescription(playlist.getDescription());
		dto.setCreatedAt(playlist.getCreatedAt());
		dto.setUpdatedAt(playlist.getUpdatedAt());
		dto.setSongCount(playlist.getSongs().size());
		return dto;
	}


}