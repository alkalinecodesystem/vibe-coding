package com.example.musicservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongResponse {
	private Long id;
	private String title;
	private Integer trackNumber;
	private Integer durationSeconds;
	private String genere;
	private String filePath;
	private String originalArtist; // artista original del tag
	private AlbumSummary album;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AlbumSummary {
		private Long id;
		private String title;
		private ArtistSummary artist;

		@Data
		@NoArgsConstructor
		@AllArgsConstructor
		public static class ArtistSummary {
			private Long id;
			private String name;
		}
	}
}
