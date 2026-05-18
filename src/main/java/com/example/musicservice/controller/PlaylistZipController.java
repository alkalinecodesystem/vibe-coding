package com.example.musicservice.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(PlaylistZipController.class);

    private final PlaylistService playlistService;

    @GetMapping("/{id}/download-advanced")
    @ResponseBody
    public void downloadPlaylistZipAdvanced(@PathVariable Long id, HttpServletResponse response) throws IOException {
        logger.info("Downloading playlist {} as zip", id);
        com.example.musicservice.dto.PlaylistResponse playlist = playlistService.getPlaylistById(id);
        List<Path> tracks = playlistService.getFilesForPlaylist(id);

        // Build M3U with playlist metadata
        StringBuilder m3u = new StringBuilder();
        m3u.append("#EXTM3U\n");
        m3u.append("#PLAYLIST:").append(playlist.getName()).append("\n");
        if (playlist.getDescription() != null && !playlist.getDescription().isEmpty()) {
            m3u.append("#DESCRIPTION:").append(playlist.getDescription()).append("\n");
        }
        m3u.append("#CREATED:").append(playlist.getCreatedAt()).append("\n");
		for (com.example.musicservice.dto.PlaylistResponse.SongSummary s : playlist.getSongs()) {
				String artist = s.getOriginalArtist();
				String displayInfo = (artist != null ? artist + " - " : "") + s.getTitle();
				m3u.append("#EXTINF:-1,").append(displayInfo).append("\n");
				String fp = s.getFilePath();
				String fileName = (fp != null && !fp.isEmpty()) ? java.nio.file.Paths.get(fp).getFileName().toString() : "";
				m3u.append(fileName).append("\n");
			}
        byte[] m3uBytes = m3u.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"playlist-" + playlist.getName() + ".zip\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            byte[] buffer = new byte[8192];
            Map<String, Integer> nameCounts = new HashMap<>();
            for (Path track : tracks) {
                if (track == null || !Files.exists(track) || !Files.isRegularFile(track)) continue;
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
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                try (InputStream in = Files.newInputStream(track)) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
            ZipEntry m3uEntry = new ZipEntry("playlist-" + playlist.getName() + ".m3u");
            zos.putNextEntry(m3uEntry);
            zos.write(m3uBytes);
            zos.closeEntry();
            zos.finish();
        } catch (IOException e) {
            logger.error("Error streaming zip for playlist {}", id, e);
            response.reset();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
