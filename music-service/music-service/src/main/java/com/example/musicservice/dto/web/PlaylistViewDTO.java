package com.example.musicservice.dto.web;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistViewDTO {
	private Long id;
	private String name;
	private String description;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private int songCount;
}