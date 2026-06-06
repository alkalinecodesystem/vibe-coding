package com.example.musicservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.musicservice.model.Artist;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {
	List<Artist> findByNameContainingIgnoreCase(String name);

	Page<Artist> findByNameContainingIgnoreCase(String name, Pageable pageable);

	Optional<Artist> findByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCase(String name);
}
