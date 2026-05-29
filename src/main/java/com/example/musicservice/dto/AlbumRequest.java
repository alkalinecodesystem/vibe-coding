package com.example.musicservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlbumRequest {
	@NotBlank(message = "Album title is required")
	@Size(min = 2, max = 200)
	private String title;

	@Min(value = 1000, message = "Release year must be at least 1000")
	@Max(value = 2100, message = "Release year cannot exceed 2100")
	private Integer releaseYear;

	@Size(max = 50)
	private String genere;

	@NotNull(message = "Artist ID is required")
	@Min(value = 1, message = "Artist ID must be positive")
	private Long artistId;
}
