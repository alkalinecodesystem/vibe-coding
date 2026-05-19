package com.example.musicservice.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import com.example.musicservice.model.Album;
import com.example.musicservice.model.Song;
import com.example.musicservice.repository.AlbumRepository;
import com.example.musicservice.exception.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlbumZipController {

    private final AlbumRepository albumRepository;

    @GetMapping("/{id}/download")
    public void downloadAlbumZip(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Album not found with id: " + id));
        List<Song> songs = album.getSongs();

        // Ordenar por trackNumber (nulls al final)
        List<Song> sortedSongs = songs.stream()
                .sorted((a, b) -> {
                    Integer ta = a.getTrackNumber();
                    Integer tb = b.getTrackNumber();
                    if (ta == null && tb == null)
                        return 0;
                    if (ta == null)
                        return 1;
                    if (tb == null)
                        return -1;
                    return ta.compareTo(tb);
                })
                .collect(Collectors.toList());

        // Build M3U con orden sortedSongs
        StringBuilder m3u = new StringBuilder();
        m3u.append("#EXTM3U\n");
        m3u.append("#ALBUM:").append(album.getTitle()).append("\n");
        if (album.getReleaseYear() != null) {
            m3u.append("#RELEASE:").append(album.getReleaseYear()).append("\n");
        }
        m3u.append("#CREATED:").append("").append("\n");

        byte[] m3uBytes;
        try {
            for (Song s : sortedSongs) {
                String artist = s.getOriginalArtist();
                String displayInfo = (artist != null ? artist + " - " : "") + s.getTitle();
                m3u.append("#EXTINF:-1,").append(displayInfo).append("\n");
                String filePath = s.getFilePath();
                String fileName = (filePath != null && !filePath.isEmpty())
                        ? Paths.get(filePath).getFileName().toString()
                        : "";
                m3u.append(fileName).append("\n");
            }
            m3uBytes = m3u.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            m3uBytes = new byte[0];
        }

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + album.getArtist().getName()+" - "+album.getTitle() + ".zip\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            byte[] buffer = new byte[8192];
            Map<String, Integer> nameCounts2 = new HashMap<>();
            for (Song s : sortedSongs) {
                String pathStr = s.getFilePath();
                if (pathStr == null || pathStr.isEmpty())
                    continue;
                Path p = Paths.get(pathStr);
                if (!Files.exists(p) || !Files.isRegularFile(p))
                    continue;

                String originalName = p.getFileName().toString();
                int count = nameCounts2.getOrDefault(originalName, 0);
                String entryName = originalName;
                if (count > 0) {
                    int dot = originalName.lastIndexOf('.');
                    String base = dot > 0 ? originalName.substring(0, dot) : originalName;
                    String ext = dot > 0 ? originalName.substring(dot) : "";
                    entryName = base + "_" + count + ext;
                }
                nameCounts2.put(originalName, count + 1);

                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                try (InputStream in = Files.newInputStream(p)) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
            ZipEntry m3uEntry = new ZipEntry(album.getArtist().getName()+" - "+album.getTitle() + ".m3u");
            zos.putNextEntry(m3uEntry);
            zos.write(m3uBytes);
            zos.closeEntry();
            zos.finish();
        } catch (IOException e) {
            System.err.println("Error streaming zip for album " + id + ": " + e.getMessage());
            response.reset();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
