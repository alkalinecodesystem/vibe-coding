package com.example.musicservice.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.musicservice.dto.PaginatedResponse;
import com.example.musicservice.dto.SongRequest;
import com.example.musicservice.dto.SongResponse;
import com.example.musicservice.dto.web.AlbumViewDTO;
import com.example.musicservice.dto.web.SongViewDTO;
import com.example.musicservice.exception.ResourceNotFoundException;
import com.example.musicservice.model.Album;
import com.example.musicservice.model.Song;
import com.example.musicservice.repository.AlbumRepository;
import com.example.musicservice.repository.SongRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SongService {

	private static final Logger logger = LoggerFactory.getLogger(SongService.class);

	private final SongRepository songRepository;
	private final AlbumRepository albumRepository;

	public SongResponse createSong(SongRequest request) {
		logger.info("Creating song: {} for album ID: {}", request.getTitle(), request.getAlbumId());

		Album album = albumRepository.findById(request.getAlbumId())
				.orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + request.getAlbumId()));

		Song song = new Song();
		song.setTitle(request.getTitle());
		song.setTrackNumber(request.getTrackNumber());
		song.setDurationSeconds(request.getDurationSeconds());
		song.setGenere(request.getGenere());
		song.setAlbum(album);

		Song saved = songRepository.save(song);
		logger.info("Song created with ID: {}", saved.getId());

		return convertToResponse(saved);
	}

	@Transactional(readOnly = true)
	public SongResponse getSongById(Long id) {
		logger.debug("Fetching song with ID: {}", id);
		Song song = songRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + id));
		return convertToResponse(song);
	}

	@Transactional(readOnly = true)
	public List<SongResponse> getAllSongs() {
		logger.debug("Fetching all songs");
		return songRepository.findAll().stream().map(this::convertToResponse).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public PaginatedResponse<SongResponse> getSongsPaginated(int page, int size, String q, String type) {
		logger.debug("Fetching songs paginated: page={}, size={}, q={}, type={}", page, size, q, type);
		Pageable pageable = PageRequest.of(page, size);
		Page<Song> songPage;

		if (StringUtils.hasText(q)) {
			if ("title".equals(type)) {
				songPage = songRepository.findByTitleContainingIgnoreCase(q, pageable);
            } else if ("album".equals(type)) {
                songPage = songRepository.findByAlbum_TitleContainingIgnoreCaseOrderByTrackNumberAsc(q, pageable);
            } else if ("artist".equals(type)) {
				songPage = songRepository.findByOriginalArtistContainingIgnoreCase(q, pageable);
			} else {
				// all fields
				List<Song> byTitle = songRepository.findByTitleContainingIgnoreCase(q);
				List<Song> byAlbum = songRepository.findByAlbum_TitleContainingIgnoreCase(q);
				List<Song> byArtist = songRepository.findByOriginalArtistContainingIgnoreCase(q);

				Set<Long> seenIds = new HashSet<>();
				List<Song> combined = new ArrayList<>();
				for (Song song : byTitle) {
					if (seenIds.add(song.getId())) {
						combined.add(song);
					}
				}
				for (Song song : byAlbum) {
					if (seenIds.add(song.getId())) {
						combined.add(song);
					}
				}
				for (Song song : byArtist) {
					if (seenIds.add(song.getId())) {
						combined.add(song);
					}
				}

				int start = (int) pageable.getOffset();
				int end = Math.min((start + pageable.getPageSize()), combined.size());
				List<Song> subList = combined.subList(start, end);
				songPage = new PageImpl<>(subList, pageable, combined.size());
			}
		} else {
			songPage = songRepository.findAll(pageable);
		}

		List<SongResponse> content = songPage.getContent().stream().map(this::convertToResponse)
				.collect(Collectors.toList());

		return new PaginatedResponse<>(content, songPage.getTotalPages(), songPage.getTotalElements(),
				songPage.getNumber(), songPage.getSize(), songPage.isFirst(), songPage.isLast(), songPage.hasNext(),
				songPage.hasPrevious());
	}

	@Transactional(readOnly = true)
	public List<SongResponse> searchSongsByTitle(String title) {
		logger.debug("Searching songs with title: {}", title);
		return songRepository.findByTitleContainingIgnoreCase(title).stream().map(this::convertToResponse)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<SongResponse> searchSongsByAlbum(String albumTitle) {
		logger.debug("Searching songs by album: {}", albumTitle);
		return songRepository.findByAlbum_TitleContainingIgnoreCase(albumTitle).stream().map(this::convertToResponse)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<SongResponse> searchSongsByArtist(String artistName) {
		logger.debug("Searching songs by original artist: {}", artistName);
		return songRepository.findByOriginalArtistContainingIgnoreCase(artistName).stream().map(this::convertToResponse)
				.collect(Collectors.toList());
	}

	public SongResponse updateSong(Long id, SongRequest request) {
		logger.info("Updating song ID: {}", id);

		Song song = songRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + id));

		Album album = albumRepository.findById(request.getAlbumId())
				.orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + request.getAlbumId()));

		song.setTitle(request.getTitle());
		song.setTrackNumber(request.getTrackNumber());
		song.setDurationSeconds(request.getDurationSeconds());
		song.setGenere(request.getGenere());
		song.setAlbum(album);

		Song saved = songRepository.save(song);
		logger.info("Song updated: {}", saved.getId());

		return convertToResponse(saved);
	}

	public void deleteSong(Long id) {
		logger.info("Deleting song ID: {}", id);

		Song song = songRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Song not found with id: " + id));

		songRepository.delete(song);
		logger.info("Song deleted: {}", id);
	}

	private SongResponse convertToResponse(Song song) {
		SongResponse response = new SongResponse();
		response.setId(song.getId());
		response.setTitle(song.getTitle());
		response.setTrackNumber(song.getTrackNumber());
		response.setDurationSeconds(song.getDurationSeconds());
		response.setGenere(song.getGenere());
		response.setFilePath(song.getFilePath());
		response.setOriginalArtist(song.getOriginalArtist());

		SongResponse.AlbumSummary albumSummary = new SongResponse.AlbumSummary();
		albumSummary.setId(song.getAlbum().getId());
		albumSummary.setTitle(song.getAlbum().getTitle());

		SongResponse.AlbumSummary.ArtistSummary artistSummary = new SongResponse.AlbumSummary.ArtistSummary();
		artistSummary.setId(song.getAlbum().getArtist().getId());
		artistSummary.setName(song.getAlbum().getArtist().getName());
		albumSummary.setArtist(artistSummary);

		response.setAlbum(albumSummary);
		return response;
	}

	@Transactional(readOnly = true)
	public List<SongViewDTO> getAllSongsForView() {
		List<Song> songs = songRepository.findAll();
		return songs.stream().map(this::convertToViewDTO).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<SongViewDTO> searchSongsForView(String query) {
		List<Song> songs = songRepository.findByTitleContainingIgnoreCase(query);
		return songs.stream().map(this::convertToViewDTO).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<SongViewDTO> searchSongsAllFieldsForView(String query) {
		List<Song> byTitle = songRepository.findByTitleContainingIgnoreCase(query);
		List<Song> byAlbum = songRepository.findByAlbum_TitleContainingIgnoreCase(query);
		List<Song> byArtist = songRepository.findByOriginalArtistContainingIgnoreCase(query);
	    List<Song> byGenere = songRepository.findByGenereContainingIgnoreCase(query);

		// Combine and remove duplicates
		java.util.Set<Long> seenIds = new java.util.HashSet<>();
		java.util.List<Song> combined = new java.util.ArrayList<>();

		java.util.List<Song> all = new java.util.ArrayList<>();
		all.addAll(byTitle);
		all.addAll(byAlbum);
		all.addAll(byArtist);
		all.addAll(byGenere);

		for (Song song : all) {
			if (seenIds.add(song.getId())) {
				combined.add(song);
			}
		}

		return combined.stream().map(this::convertToViewDTO).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public Page<SongViewDTO> getSongsForViewPaginated(Pageable pageable) {
		Page<Song> songs = songRepository.findAll(pageable);
		return songs.map(this::convertToViewDTO);
	}

	@Transactional(readOnly = true)
	public Page<SongViewDTO> searchSongsForViewPaginated(String query, Pageable pageable) {
		Page<Song> songs = songRepository.findByTitleContainingIgnoreCase(query, pageable);
		return songs.map(this::convertToViewDTO);
	}

	@Transactional(readOnly = true)
	public Page<SongViewDTO> searchSongsByAlbumForViewPaginated(String albumTitle, Pageable pageable) {
		Page<Song> songs = songRepository.findByAlbum_TitleContainingIgnoreCaseOrderByTrackNumberAsc(albumTitle, pageable);
		return songs.map(this::convertToViewDTO);
	}

	@Transactional(readOnly = true)
	public Page<SongViewDTO> searchSongsByArtistForViewPaginated(String originalArtist, Pageable pageable) {
		Page<Song> songs = songRepository.findByOriginalArtistContainingIgnoreCase(originalArtist, pageable);
		return songs.map(this::convertToViewDTO);
	}
    
	@Transactional(readOnly = true)
	public Page<SongViewDTO> searchSongsByGenereForViewPaginated(String originalArtist, Pageable pageable) {
		Page<Song> songs = songRepository.findByGenereContainingIgnoreCase(originalArtist, pageable);
		return songs.map(this::convertToViewDTO);
	}

	@Transactional(readOnly = true)
	public Page<SongViewDTO> searchSongsAllFieldsForViewPaginated(String query, Pageable pageable) {
		List<SongViewDTO> allSongs = searchSongsAllFieldsForView(query);
		int start = (int) pageable.getOffset();
		int end = Math.min((start + pageable.getPageSize()), allSongs.size());
		List<SongViewDTO> subList = allSongs.subList(start, end);
		return new PageImpl<>(subList, pageable, allSongs.size());
	}

	private SongViewDTO convertToViewDTO(Song song) {
		SongViewDTO dto = new SongViewDTO();
		dto.setId(song.getId());
		dto.setTitle(song.getTitle());
		dto.setTrackNumber(song.getTrackNumber());
		dto.setDurationSeconds(song.getDurationSeconds());
		dto.setGenere(song.getGenere());
		dto.setFilePath(song.getFilePath());
		dto.setOriginalArtist(song.getOriginalArtist());

		if (song.getAlbum() != null) {
			Album album = song.getAlbum();
			AlbumViewDTO albumDTO = new AlbumViewDTO();
			albumDTO.setId(album.getId());
			albumDTO.setTitle(album.getTitle());
			albumDTO.setArtistName(album.getArtist() != null ? album.getArtist().getName() : "Unknown");
			dto.setAlbum(albumDTO);
		}
		return dto;
	}
}
