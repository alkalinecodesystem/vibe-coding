package com.example.musicservice.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "albums")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Album {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Album title is required")
	@Size(min = 2, max = 200, message = "Title must be between 2 and 200 characters")
	@Column(nullable = false, unique = true)
	private String title;

	@Column(name = "release_year")
	private Integer releaseYear;

	@Size(max = 50, message = "Genere cannot exceed 50 characters")
	@Column(length = 50)
	private String genere;

	@Lob
	@Column(name = "cover_image")
	private byte[] coverImage;

	@Column(name = "cover_content_type")
	private String coverContentType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "artist_id", nullable = false)
	private Artist artist;

	@OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Song> songs = new ArrayList<>();

	public void addSong(Song song) {
		songs.add(song);
		song.setAlbum(this);
	}

	public void removeSong(Song song) {
		songs.remove(song);
		song.setAlbum(null);
	}
}
