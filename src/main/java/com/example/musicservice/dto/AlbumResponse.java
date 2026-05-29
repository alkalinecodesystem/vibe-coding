package com.example.musicservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlbumResponse {
	private Long id;
	private String title;
	private Integer releaseYear;
	private String genere;
	private ArtistSummary artist;
	private List<SongSummary> songs;
	private boolean hasCover;

	// Transient fields for displaying cover in HTML (not part of the API response
	// normally)
	private transient byte[] coverImage;
	private transient String coverContentType;

	public void setCoverImageBase64(String base64) {
		// Helper method not needed, will be set via field
	}

	public String getCoverImageBase64() {
		if (coverImage != null) {
			return java.util.Base64.getEncoder().encodeToString(coverImage);
		}
		return null;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ArtistSummary {
		private Long id;
		private String name;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SongSummary {
		private Long id;
		private String title;
		private Integer trackNumber;
		private Integer durationSeconds;
		private String genere;
		private String filePath; // Add file path for audio playback
		private String originalArtist;
		private String formattedDuration;
	}
}
