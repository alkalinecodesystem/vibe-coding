package com.example.musicservice.controller;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.musicservice.dto.web.AlbumViewDTO;
import com.example.musicservice.dto.web.SongViewDTO;
import com.example.musicservice.service.AlbumService;
import com.example.musicservice.service.PlaylistService;
import com.example.musicservice.service.SongService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ThymeleafController {

	private static final Logger logger = LoggerFactory.getLogger(ThymeleafController.class);

	private final AlbumService albumService;
	private final SongService songService;
	private final PlaylistService playlistService;

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("title", "Music Manager - Dashboard");

		List<AlbumViewDTO> allAlbums = albumService.getAllAlbumsForView();
		List<SongViewDTO> allSongs = songService.getAllSongsForView();

		long coversCount = allAlbums.stream().filter(AlbumViewDTO::isHasCover).count();

		// Get random 10 albums for main page
		List<AlbumViewDTO> randomAlbums = allAlbums.stream()
				.collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
					Collections.shuffle(list);
					return list.stream().limit(10).toList();
				}));

		long artistCount = allAlbums.stream().map(AlbumViewDTO::getArtistName).distinct().count();

		model.addAttribute("totalArtists", artistCount);
		model.addAttribute("totalAlbums", allAlbums.size());
		model.addAttribute("totalSongs", allSongs.size());
		model.addAttribute("totalCovers", coversCount);
		model.addAttribute("randomAlbums", randomAlbums);
		model.addAttribute("activeMenu", "dashboard");
		model.addAttribute("contentTemplate", "fragments/dashboard");
		model.addAttribute("contentFragment", "content");
		return "layout";
	}

	@GetMapping("/albums")
	public String albums(Model model, @RequestParam(value = "q", required = false) String searchQuery,
			@RequestParam(value = "type", required = false, defaultValue = "all") String searchType) {
		model.addAttribute("title", "Albums - Music Manager");

		List<AlbumViewDTO> albums;
		if (searchQuery != null && !searchQuery.trim().isEmpty()) {
			String query = searchQuery.trim();
			switch (searchType.toLowerCase()) {
			case "title":
				albums = albumService.searchAlbumsByTitleForView(query);
				break;
			case "artist":
				albums = albumService.searchAlbumsByArtistForView(query);
				break;
			case "song":
				albums = albumService.searchAlbumsBySongForView(query);
				break;
			case "all":
			default:
				albums = albumService.searchAlbumsAllFieldsForView(query);
				break;
			}
		} else {
			// Show random 10 albums as main feature
			albums = albumService.getAllAlbumsForView().stream()
					.collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
						Collections.shuffle(list);
						return list.stream().limit(10).toList();
					}));
		}

		model.addAttribute("albums", albums);
		model.addAttribute("searchQuery", searchQuery);
		model.addAttribute("searchType", searchType);
		model.addAttribute("activeMenu", "albums");
		model.addAttribute("contentTemplate", "fragments/albums");
		model.addAttribute("contentFragment", "content");
		return "layout";
	}

	@GetMapping("/albums/{id}")
	public String albumDetail(@PathVariable Long id, Model model) {
		AlbumViewDTO album = albumService.getAlbumViewById(id);
		model.addAttribute("title", album.getTitle() + " - Album Detail");
		model.addAttribute("album", album);
		model.addAttribute("filesDeleted", false);
		model.addAttribute("contentTemplate", "fragments/album-detail");
		model.addAttribute("contentFragment", "content");
		return "layout";
	}

	@PostMapping("/albums/{id}/delete")
	public String deleteAlbum(@PathVariable Long id) {
		logger.info("Deleting album via web controller: {}", id);
		albumService.deleteAlbum(id);
		return "redirect:/albums";
	}

	@PostMapping("/albums/{id}/delete-files")
	public String deleteAlbumFiles(@PathVariable Long id, Model model) {
		logger.info("Deleting files for album via web controller: {}", id);
		albumService.deleteAlbumFiles(id);
		AlbumViewDTO album = albumService.getAlbumViewById(id);
		model.addAttribute("title", album.getTitle() + " - Album Detail");
		model.addAttribute("album", album);
		model.addAttribute("filesDeleted", true);
		model.addAttribute("contentTemplate", "fragments/album-detail");
		model.addAttribute("contentFragment", "content");
		return "layout";
	}

	@GetMapping("/songs")
	public String songs(Model model, @RequestParam(value = "q", required = false) String searchQuery,
			@RequestParam(value = "type", required = false, defaultValue = "all") String searchType,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		model.addAttribute("title", "Songs - Music Manager");
		model.addAttribute("searchQuery", searchQuery);
		model.addAttribute("searchType", searchType);

		// Validate size: only allow 10, 20, 30
		if (size != 10 && size != 20 && size != 30) {
			size = 10;
		}

		Pageable pageable = PageRequest.of(page, size);

		Page<SongViewDTO> songPage;
		if (searchQuery != null && !searchQuery.trim().isEmpty()) {
			String query = searchQuery.trim();
			switch (searchType.toLowerCase()) {
			case "album":
				songPage = songService.searchSongsByAlbumForViewPaginated(query, pageable);
				break;
			case "artist":
				songPage = songService.searchSongsByArtistForViewPaginated(query, pageable);
				break;
			case "all":
				songPage = songService.searchSongsAllFieldsForViewPaginated(query, pageable);
				break;
			case "title":
			default:
				songPage = songService.searchSongsForViewPaginated(query, pageable);
				break;
			}
		} else {
			songPage = songService.getSongsForViewPaginated(pageable);
		}

		model.addAttribute("songs", songPage.getContent());
		model.addAttribute("currentPage", songPage.getNumber());
		model.addAttribute("totalPages", songPage.getTotalPages());
		model.addAttribute("totalElements", songPage.getTotalElements());
		model.addAttribute("size", size);
		model.addAttribute("hasNext", songPage.hasNext());
		model.addAttribute("hasPrevious", songPage.hasPrevious());
		model.addAttribute("isFirst", songPage.isFirst());
		model.addAttribute("isLast", songPage.isLast());
		model.addAttribute("activeMenu", "songs");
		model.addAttribute("contentTemplate", "fragments/songs");
		model.addAttribute("contentFragment", "content");
		return "layout";
	}

	@GetMapping("/playlists")
	public String playlists(Model model) {
		model.addAttribute("title", "Playlists - Music Manager");
		model.addAttribute("playlists", playlistService.getAllPlaylistsForView());
		model.addAttribute("activeMenu", "playlists");
		model.addAttribute("contentTemplate", "fragments/playlists");
		model.addAttribute("contentFragment", "content");
		return "layout";
	}

	@GetMapping("/playlists/{id}")
	public String playlistDetail(@PathVariable Long id, Model model) {
		model.addAttribute("title", "Playlist Detail - Music Manager");
		model.addAttribute("playlist", playlistService.getPlaylistById(id));
		model.addAttribute("activeMenu", "playlists");
		model.addAttribute("contentTemplate", "fragments/playlist-detail");
		model.addAttribute("contentFragment", "content");
		return "layout";
	}

	@GetMapping("/upload")
	public String upload(Model model) {
		model.addAttribute("title", "Upload ZIP - Music Manager");
		model.addAttribute("activeMenu", "upload");
		model.addAttribute("contentTemplate", "fragments/upload");
		model.addAttribute("contentFragment", "content");
		return "layout";
	}
}
