package com.example.musicservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.musicservice.model.Playlist;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
	List<Playlist> findByNameContainingIgnoreCase(String name);

	List<Playlist> findAllByOrderByCreatedAtDesc();
}