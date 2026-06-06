package com.example.musicservice.dto;

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
}
