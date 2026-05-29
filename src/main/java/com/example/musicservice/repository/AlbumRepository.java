package com.example.musicservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.musicservice.model.Album;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
	List<Album> findByTitleContainingIgnoreCase(String title);

	List<Album> findByArtist_NameContainingIgnoreCase(String artistName);

	List<Album> findBySongs_TitleContainingIgnoreCase(String songTitle);

	Optional<Album> findByTitleIgnoreCaseAndArtist_Id(String title, Long artistId);

	Optional<Album> findByTitleIgnoreCase(String title);

	Page<Album> findByCoverImageIsNotNull(Pageable pageable);

	@Query("SELECT DISTINCT a FROM Album a WHERE a.id = :id")
	Optional<Album> findByIdWithSongs(@Param("id") Long id);

	List<Album> findBySongs_OriginalArtistContainingIgnoreCase(String songOriginalArtist);

	List<Album> findBySongs_GenereContainingIgnoreCase(String songGenere);

}
