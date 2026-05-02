package com.example.musicservice.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.musicservice.dto.UploadResponse;
import com.example.musicservice.exception.InvalidFileException;
import com.example.musicservice.model.Album;
import com.example.musicservice.model.Artist;
import com.example.musicservice.model.Song;
import com.example.musicservice.repository.AlbumRepository;
import com.example.musicservice.repository.ArtistRepository;
import com.example.musicservice.repository.SongRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ZipUploadService {

	private static final Logger logger = LoggerFactory.getLogger(ZipUploadService.class);

	private final ArtistRepository artistRepository;
	private final AlbumRepository albumRepository;
	private final SongRepository songRepository;

	@Value("${app.upload.extracted-dir:/tmp/music-extracted}")
	private String extractedDirPath;

	private static final List<String> SUPPORTED_EXTENSIONS = List.of("mp3", "flac", "ogg", "wav", "m4a");

	@Transactional
	public UploadResponse processZipFile(MultipartFile file) {
		logger.info("Processing ZIP file: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

		validateZipFile(file);

		Path extractionDir = null;
		final int[] counters = new int[4]; // [0]=artistsCreated, [1]=albumsCreated, [2]=songsCreated, [3]=songsSkipped

		try {
			// Create base extracted directory if it doesn't exist
			Path baseDir = Paths.get(extractedDirPath);
			if (!Files.exists(baseDir)) {
				Files.createDirectories(baseDir);
				logger.info("Created base extraction directory: {}", baseDir);
			}

			// Create a unique subdirectory for this upload (timestamp + UUID)
			String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
			String uploadId = UUID.randomUUID().toString().substring(0, 8);
			String subDirName = String.format("%s_%s", timestamp, uploadId);
			extractionDir = baseDir.resolve(subDirName);
			Files.createDirectories(extractionDir);

			logger.info("Extracting ZIP to: {}", extractionDir);

			// Extract ZIP
			extractZipFile(file.getInputStream(), extractionDir);

			// Process audio files
			List<File> audioFiles = findAudioFiles(extractionDir.toFile());

			logger.info("Found {} audio files to process", audioFiles.size());

			// Keep track of processed artist/album combinations to avoid duplicates
			Set<String> processedAlbums = new HashSet<>();

			for (File audioFile : audioFiles) {
				try {
					AudioFile audio = AudioFileIO.read(audioFile);
					Tag tag = audio.getTag();

					if (tag == null) {
						logger.warn("No tags found in file: {}", audioFile.getName());
						counters[3]++;
						continue;
					}

					String artistName = tag.getFirst(FieldKey.ARTIST);
					String albumTitle = tag.getFirst(FieldKey.ALBUM);
					String songTitle = tag.getFirst(FieldKey.TITLE);
					String trackStr = tag.getFirst(FieldKey.TRACK);
					String durationStr = String.valueOf(audio.getAudioHeader().getTrackLength());
					String genre = tag.getFirst(FieldKey.GENRE);

					if (artistName == null || artistName.trim().isEmpty()) {
						logger.warn("Missing artist tag in file: {}", audioFile.getName());
						counters[3]++;
						continue;
					}

					if (albumTitle == null || albumTitle.trim().isEmpty()) {
						logger.warn("Missing album tag in file: {}", audioFile.getName());
						counters[3]++;
						continue;
					}

					if (songTitle == null || songTitle.trim().isEmpty()) {
						songTitle = audioFile.getName();
					}

					// Parse track number
					Integer trackNumber = null;
					if (trackStr != null && !trackStr.trim().isEmpty()) {
						try {
							trackNumber = Integer.parseInt(trackStr.split("/")[0].trim());
						} catch (NumberFormatException e) {
							logger.warn("Invalid track number: {}", trackStr);
						}
					}

					// Parse duration
					Integer duration = null;
					try {
						duration = Integer.parseInt(durationStr);
					} catch (NumberFormatException e) {
						logger.warn("Invalid duration: {}", durationStr);
					}

					// Create or find artist (artist from this specific audio file)
					Artist artist = artistRepository.findByNameIgnoreCase(artistName.trim()).orElseGet(() -> {
						Artist newArtist = new Artist();
						newArtist.setName(artistName.trim());
						artistRepository.save(newArtist);
						counters[0]++;
						logger.info("Created new artist: {}", artistName);
						return newArtist;
					});

					// Create album key (only by title, case-insensitive)
					String albumKey = albumTitle.trim().toLowerCase();

					Album album;
					if (!processedAlbums.contains(albumKey)) {
						// Check if album exists globally by title (ignore case)
						album = albumRepository.findByTitleIgnoreCase(albumTitle.trim()).orElseGet(() -> {
							Album newAlbum = new Album();
							newAlbum.setTitle(albumTitle.trim());
							// Set the artist from this first occurrence
							newAlbum.setArtist(artist);
							// Extract genre, year, cover as before
							newAlbum.setGenre(genre != null ? genre.trim() : null);
							String year = tag.getFirst(FieldKey.YEAR);
							if (year != null && !year.trim().isEmpty()) {
								try {
									newAlbum.setReleaseYear(Integer.parseInt(year.trim()));
								} catch (NumberFormatException e) {
									// ignore
								}
							}
							// Extract embedded album art if available
							try {
								Artwork artwork = tag.getFirstArtwork();
								if (artwork != null && artwork.getBinaryData() != null) {
									newAlbum.setCoverImage(artwork.getBinaryData());
									String mimeType = artwork.getMimeType();
									if (mimeType != null) {
										newAlbum.setCoverContentType(mimeType);
									} else {
										newAlbum.setCoverContentType("image/jpeg");
									}
								}
							} catch (Exception e) {
								logger.warn("Could not extract embedded artwork for album: {}", albumTitle, e);
							}
							albumRepository.save(newAlbum);
							counters[1]++;
							logger.info("Created new album: {} with artist: {}", albumTitle, artistName);
							return newAlbum;
						});

						// If album exists but has no artist, set it (first occurrence)
						if (album.getArtist() == null) {
							album.setArtist(artist);
							albumRepository.save(album);
							logger.info("Set album artist to: {} for album: {}", artistName, albumTitle);
						}

						processedAlbums.add(albumKey);
					} else {
						// Album already processed, fetch it
						album = albumRepository.findByTitleIgnoreCase(albumTitle.trim())
								.orElseThrow(() -> new IllegalStateException("Album not found after creation"));
					}

					// Determine if this song's artist differs from album's artist
					String originalArtistToStore = null;
					if (!artist.getId().equals(album.getArtist().getId())) {
						// Different artist: mark album as Various Artists (VA)
						if (!"VA".equals(album.getArtist().getName())) {
							// Find or create VA artist
							Artist vaArtist = artistRepository.findByNameIgnoreCase("VA").orElseGet(() -> {
								Artist va = new Artist();
								va.setName("VA");
								artistRepository.save(va);
								logger.info("Created VA artist");
								return va;
							});
							album.setArtist(vaArtist);
							albumRepository.save(album);
							logger.info("Album {} marked as VA due to multiple artists", albumTitle);
						}
						// Store the song's original artist
						originalArtistToStore = artist.getName();
					} else {
						originalArtistToStore = artist.getName();
					}

					// Create song
					Song song = new Song();
					song.setTitle(songTitle.trim());
					song.setTrackNumber(trackNumber);
					song.setDurationSeconds(duration);
					song.setGenre(genre != null ? genre.trim() : null);
					song.setFilePath(audioFile.getAbsolutePath());
					song.setOriginalArtist(originalArtistToStore);
					song.setAlbum(album);
					songRepository.save(song);
					counters[2]++;

					logger.debug("Processed song: {}", songTitle);

				} catch (Exception e) {
					logger.error("Error processing file: " + audioFile.getName(), e);
					counters[3]++;
				}
			}

			logger.info("ZIP processing complete. Artists created: {}, Albums: {}, Songs: {}, Skipped: {}", counters[0],
					counters[1], counters[2], counters[3]);

			String message = String.format(
					"Processed %d audio files. Created: %d artists, %d albums, %d songs. Skipped: %d files",
					audioFiles.size(), counters[0], counters[1], counters[2], counters[3]);

			return new UploadResponse(true, message, extractionDir.toString());

		} catch (IOException e) {
			logger.error("Error processing ZIP file", e);
			throw new InvalidFileException("Failed to process ZIP file: " + e.getMessage());
		} finally {
			// Files are kept persistently, no cleanup
			logger.debug("Extracted files remain at: {}", extractionDir);
		}
	}

	private void validateZipFile(MultipartFile file) {
		if (file.isEmpty()) {
			throw new InvalidFileException("File is empty");
		}

		String filename = file.getOriginalFilename();
		if (filename == null || !filename.toLowerCase().endsWith(".zip")) {
			throw new InvalidFileException("Only ZIP files are allowed");
		}
	}

	private void extractZipFile(InputStream inputStream, Path targetDir) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(inputStream)) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				// Security: prevent path traversal attacks
				String entryName = entry.getName();
				if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
					logger.warn("Skipping potentially malicious ZIP entry: {}", entryName);
					continue;
				}

				Path newFile = targetDir.resolve(entryName).normalize();

				// Ensure the resolved path is still within targetDir
				if (!newFile.startsWith(targetDir)) {
					logger.warn("Skipping path traversal attempt: {}", entryName);
					continue;
				}

				if (entry.isDirectory()) {
					Files.createDirectories(newFile);
				} else {
					Path parent = newFile.getParent();
					if (parent != null && !Files.exists(parent)) {
						Files.createDirectories(parent);
					}
					Files.copy(zis, newFile);
				}
				zis.closeEntry();
			}
		}
	}

	private List<File> findAudioFiles(File directory) {
		List<File> audioFiles = new ArrayList<>();
		findAudioFilesRecursive(directory, audioFiles);
		return audioFiles;
	}

	private void findAudioFilesRecursive(File directory, List<File> audioFiles) {
		File[] files = directory.listFiles();
		if (files == null)
			return;

		for (File file : files) {
			if (file.isDirectory()) {
				findAudioFilesRecursive(file, audioFiles);
			} else {
				String filename = file.getName().toLowerCase();
				for (String ext : SUPPORTED_EXTENSIONS) {
					if (filename.endsWith("." + ext)) {
						audioFiles.add(file);
						break;
					}
				}
			}
		}
	}
}
