package com.example.musicservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.musicservice.dto.ApiResponse;
import com.example.musicservice.dto.UploadAnalysisResponse;
import com.example.musicservice.dto.UploadResponse;
import com.example.musicservice.service.ZipUploadService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Validated
public class UploadController {

	private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

	private final ZipUploadService zipUploadService;

	@PostMapping("/zip")
	public ResponseEntity<ApiResponse<?>> uploadZipFile(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "force", required = false, defaultValue = "false") boolean force) {

		logger.info("Received ZIP upload request: {}, content-type: {}", file.getOriginalFilename(),
				file.getContentType());

		if (file.isEmpty()) {
			throw new IllegalArgumentException("File cannot be empty");
		}

		String filename = file.getOriginalFilename();
		if (filename == null || !filename.toLowerCase().endsWith(".zip")) {
			throw new IllegalArgumentException("Only ZIP files are allowed");
		}

		if (!force) {
			UploadAnalysisResponse analysis = zipUploadService.analyzeZipFile(file);
			if (analysis.isHasExistingAlbums()) {
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body(ApiResponse.success(
								"Some albums already exist. Set force=true for continue.", analysis));
			}
		}

		UploadResponse result = zipUploadService.processZipFile(file);
		return ResponseEntity.ok(ApiResponse.success("File uploaded and processed successfully", result));
	}
}
