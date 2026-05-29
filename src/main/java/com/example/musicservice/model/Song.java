package com.example.musicservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "songs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Song {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Song title is required")
	@Size(min = 2, max = 200, message = "Title must be between 2 and 200 characters")
	@Column(nullable = false)
	private String title;

	@Column(name = "track_number")
	private Integer trackNumber;

	@Column(name = "duration_seconds")
	private Integer durationSeconds;

	@Size(max = 50, message = "Genere cannot exceed 50 characters")
	private String genere;

	@Transient
	public String getFormattedDuration() {
		if (durationSeconds == null) {
			return "-";
		}
		int minutes = durationSeconds / 60;
		int seconds = durationSeconds % 60;
		return String.format("%d:%02d", minutes, seconds);
	}

	@Column(name = "file_path", length = 500)
	private String filePath;

	@Column(name = "original_artist", length = 100)
	private String originalArtist;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "album_id", nullable = false)
	private Album album;
}
