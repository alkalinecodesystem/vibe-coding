package com.example.musicservice.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistResponse {
	private Long id;
	private String name;
	private String description;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private int songCount;
	private List<SongSummary> songs;

	// Transient fields for displaying cover in HTML (not part of the API response)
	private transient byte[] coverImage;
	private transient String coverContentType;

	public String getCoverImageBase64() {
		if (coverImage != null) {
			return java.util.Base64.getEncoder().encodeToString(coverImage);
		}
		return null;
	}

	public boolean isHasCover() {
		return coverImage != null && coverImage.length > 0;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SongSummary {
		private Long id;
		private String title;
		private Integer trackNumber;
		private String formattedDuration;
		private String genre;
		private String originalArtist;
		private String filePath;
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
}