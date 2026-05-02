package com.example.musicservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtistResponse {
	private Long id;
	private String name;
	private String biography;
	private List<AlbumSummary> albums;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AlbumSummary {
		private Long id;
		private String title;
		private Integer releaseYear;
	}
}
