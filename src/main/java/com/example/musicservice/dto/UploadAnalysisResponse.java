package com.example.musicservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadAnalysisResponse {

	private boolean hasExistingAlbums;
	private List<String> existingAlbums;
	private List<String> newAlbums;
	private int totalAlbumsInZip;
	private String message;
}