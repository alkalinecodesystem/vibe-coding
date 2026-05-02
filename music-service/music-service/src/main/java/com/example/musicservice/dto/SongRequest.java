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
public class SongRequest {
	@NotBlank(message = "Song title is required")
	@Size(min = 2, max = 200)
	private String title;

	@Min(value = 1, message = "Track number must be positive")
	@Max(value = 999, message = "Track number cannot exceed 999")
	private Integer trackNumber;

	@Min(value = 0, message = "Duration cannot be negative")
	private Integer durationSeconds;

	@Size(max = 50)
	private String genre;

	@NotNull(message = "Album ID is required")
	@Min(value = 1, message = "Album ID must be positive")
	private Long albumId;
}
