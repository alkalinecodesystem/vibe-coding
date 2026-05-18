package com.example.musicservice.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.musicservice.dto.PlaylistResponse;
import com.example.musicservice.service.PlaylistService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistZipController {

    private final PlaylistService playlistService;

    @GetMapping("/{id}/download-advanced")
    @ResponseBody
    public void downloadPlaylistZipAdvanced(@PathVariable Long id, HttpServletResponse response) throws IOException {
        PlaylistResponse playlist = playlistService.getPlaylistById(id);
        List<Path> tracks = playlistService.getFilesForPlaylist(id);

        // Build M3U content inside ZIP
        StringBuilder m3u = new StringBuilder();
        m3u.append("#EXTM3U\n");
        m3u.append("#PLAYLIST:").append(playlist.getName()).append("\n");
        if (playlist.getDescription() != null && !playlist.getDescription().isEmpty()) {
            m3u.append("#DESCRIPTION:").append(playlist.getDescription()).append("\n");
        }
        m3u.append("#CREATED:").append(playlist.getCreatedAt()).append("\n");
        for (PlaylistResponse.SongSummary s : playlist.getSongs()) {
            String artist = s.getOriginalArtist();
            String displayInfo = (artist != null ? artist + " - " : "") + s.getTitle();
            m3u.append("#EXTINF:-1,").append(displayInfo).append("\n");
            m3u.append(s.getFilePath()).append("\n");
        }
        byte[] m3uBytes = m3u.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"playlist-" + id + ".zip\"");

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(response.getOutputStream())) {
            byte[] buffer = new byte[8192];
            java.util.Map<String, Integer> nameCounts = new java.util.HashMap<>();
            for (Path track : tracks) {
                if (track == null || !java.nio.file.Files.exists(track) || !java.nio.file.Files.isRegularFile(track)) continue;
                String originalName = track.getFileName().toString();
                int count = nameCounts.getOrDefault(originalName, 0);
                String entryName = originalName;
                if (count > 0) {
                    int dot = originalName.lastIndexOf('.');
                    String base = dot > 0 ? originalName.substring(0, dot) : originalName;
                    String ext = dot > 0 ? originalName.substring(dot) : "";
                    entryName = base + "_" + count + ext;
                }
                nameCounts.put(originalName, count + 1);
                java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName);
                zos.putNextEntry(entry);
                try (java.io.InputStream in = java.nio.file.Files.newInputStream(track)) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
            java.util.zip.ZipEntry m3uEntry = new java.util.zip.ZipEntry("playlist-" + id + ".m3u");
            zos.putNextEntry(m3uEntry);
            zos.write(m3uBytes);
            zos.closeEntry();
            zos.finish();
        } catch (IOException e) {
            // Minimal error handling: log to stderr to keep it self-contained
            System.err.println("Error streaming zip for playlist " + id + ": " + e.getMessage());
            response.reset();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
