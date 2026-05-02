package com.example.musicservice.dto.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongViewDTO {
	private Long id;
	private String title;
	private Integer trackNumber;
	private Integer durationSeconds;
	private String genre;
	private String filePath;
	private String originalArtist; // artista original del tag MP3
	private AlbumViewDTO album;

	public String getFormattedDuration() {
		if (durationSeconds == null) {
			return "-";
		}
		int minutes = durationSeconds / 60;
		int seconds = durationSeconds % 60;
		return String.format("%d:%02d", minutes, seconds);
	}
}
