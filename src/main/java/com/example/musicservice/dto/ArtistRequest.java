package com.example.musicservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtistRequest {
	@NotBlank(message = "Artist name is required")
	@Size(min = 2, max = 100)
	private String name;

	@Size(max = 1000)
	private String biography;
}
