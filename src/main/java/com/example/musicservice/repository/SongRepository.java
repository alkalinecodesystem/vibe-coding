package com.example.musicservice.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.musicservice.model.Song;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {
	List<Song> findByTitleContainingIgnoreCase(String title);

	List<Song> findByAlbum_TitleContainingIgnoreCase(String albumTitle);

	List<Song> findByOriginalArtistContainingIgnoreCase(String originalArtist);

	List<Song> findByGenereContainingIgnoreCase(String genere);

	Page<Song> findByTitleContainingIgnoreCase(String title, Pageable pageable);

	Page<Song> findByAlbum_TitleContainingIgnoreCaseOrderByTrackNumberAsc(String albumTitle, Pageable pageable);

	Page<Song> findByOriginalArtistContainingIgnoreCase(String originalArtist, Pageable pageable);
    
	Page<Song> findByGenereContainingIgnoreCase(String genere, Pageable pageable);

	@Query("SELECT COUNT(DISTINCT s.originalArtist) FROM Song s")
    long countDistinctOriginalArtist();
}
