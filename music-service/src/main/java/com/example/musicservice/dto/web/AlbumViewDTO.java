package com.example.musicservice.dto.web;

import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlbumViewDTO {
	private Long id;
	private String title;
	private Integer releaseYear;
	private String genre;
	private String artistName;
	private Integer songCount;
	private boolean hasCover;
	private String coverContentType;
	private String coverImageBase64;
	private List<SongViewDTO> songs;

	/**
	 * Returns a space-separated string of all song titles for client-side search.
	 */
	public String getSongTitlesCsv() {
		if (songs == null || songs.isEmpty()) {
			return "";
		}
		return songs.stream().map(SongViewDTO::getTitle).filter(title -> title != null)
				.collect(Collectors.joining(" "));
	}
}
