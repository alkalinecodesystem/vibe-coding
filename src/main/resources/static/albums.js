// Player state lives at top level (module scope) so it survives re-calls to initAlbums()
let currentAudioPlayer = null;
let currentSongIndex = -1;
let currentAlbumSongs = [];
let isPlaying = false;
let isShuffle = false;
let repeatMode = 'none'; // 'none' | 'all' | 'one'
let currentVolume = 0.5;
let currentTriggerButton = null;
let playerControlsInitialized = false;

function initAlbums() {
  const searchInput = document.getElementById('search-input');
  const resultsCount = document.getElementById('search-results-count');
  const matchingCountSpan = document.getElementById('matching-count');
  const totalCountSpan = document.getElementById('total-count');
  const albumCards = document.querySelectorAll('.col-md-3.col-sm-6.mb-4');

  const albumsData = Array.from(albumCards).map(card => {
    const titleEl = card.querySelector('h6');
    const artistEl = card.querySelector('p.text-muted');
    const songsEl = card.querySelector('.badge');
    return {
      element: card,
      title: titleEl ? titleEl.textContent.trim() : '',
      artist: artistEl ? artistEl.textContent.trim() : '',
      songs: songsEl ? songsEl.textContent.trim() : ''
    };
  });

  totalCountSpan.textContent = albumsData.length;

  function performSearch(query) {
    const normalizedQuery = query.toLowerCase().trim();
    let matchCount = 0;

    albumsData.forEach(album => {
      const matchesAlbum = album.title.toLowerCase().includes(normalizedQuery);
      const matchesArtist = album.artist.toLowerCase().includes(normalizedQuery);
      const matchesSong = album.songs.toLowerCase().includes(normalizedQuery);

      const isVisible = matchesAlbum || matchesArtist || matchesSong;
      album.element.style.display = isVisible ? '' : 'none';
      if (isVisible) matchCount++;
    });

    matchingCountSpan.textContent = matchCount;
    if (query) {
      resultsCount.style.display = 'block';
    } else {
      resultsCount.style.display = 'none';
    }
  }

  if (searchInput) {
    if (!searchInput.dataset.searchListenerAttached) {
      searchInput.dataset.searchListenerAttached = 'true';
      let debounceTimer;
      searchInput.addEventListener('input', function() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
          performSearch(this.value);
        }, 150);
      });
    }
  }

  const playAlbumButtons = document.querySelectorAll('.play-album-btn');
  playAlbumButtons.forEach(button => {
    if (button.dataset.playAlbumListenerAttached === 'true') return;
    button.dataset.playAlbumListenerAttached = 'true';
    button.addEventListener('click', async function () {
      currentTriggerButton = this;
      const albumId = this.getAttribute('data-album-id');
      const albumTitle = this.getAttribute('data-album-title');
      const albumArtist = this.getAttribute('data-album-artist');
      if (!albumId) {
        alert('Error: No album ID found.');
        return;
      }

      try {
        const response = await fetch(`/api/albums/${albumId}`);
        if (!response.ok) {
          throw new Error('Failed to load album');
        }
        const responseJson = await response.json();
        const data = responseJson.data;

        document.getElementById('modal-album-title').textContent = albumTitle;
        document.getElementById('modal-album-artist').textContent = albumArtist;

        const coverImg = document.getElementById('modal-album-cover');
        if (data.hasCover && data.coverContentType && data.coverImageBase64) {
          coverImg.src = 'data:' + data.coverContentType + ';base64,' + data.coverImageBase64;
          coverImg.style.display = 'center';
        } else {
          coverImg.style.display = 'none';
        }

        currentAlbumSongs = data.songs || [];
        currentAlbumSongs.sort((a, b) => {
          const ta = a.trackNumber != null ? a.trackNumber : Number.MAX_VALUE;
          const tb = b.trackNumber != null ? b.trackNumber : Number.MAX_VALUE;
          return ta - tb;
        });

        const songListContainer = document.getElementById('modal-song-list');
        songListContainer.innerHTML = '';

        if (currentAlbumSongs.length > 0) {
          document.getElementById('audio-controls').style.display = 'block';
          const table = document.createElement('table');
          table.className = 'table table-hover mb-0';
          table.innerHTML = `
            <thead class="table-light">
              <tr><th>#</th><th>Title</th><th>Track</th><th>Duration</th><th>Genre</th><th>Artist</th><th>Play</th></tr>
            </thead>
            <tbody></tbody>
          `;
          const tbody = table.querySelector('tbody');
          currentAlbumSongs.forEach((song, idx) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
              <td>${idx + 1}</td>
              <td><strong>${song.title || ''}</strong></td>
              <td>${song.trackNumber ?? '-'}</td>
              <td>${song.formattedDuration ?? ''}</td>
              <td>${song.genre ? `<span class="badge bg-secondary">${song.genre}</span>` : '-'}</td>
              <td>${song.originalArtist ?? (data.originalArtist || '')}</td>
              <td><button class="btn btn-sm btn-outline-primary play-song-btn" data-song-index="${idx}"><i class="bi bi-play-circle"></i></button></td>
            `;
            tbody.appendChild(tr);
          });
          songListContainer.appendChild(table);
        } else {
          document.getElementById('audio-controls').style.display = 'none';
          songListContainer.innerHTML = '<p class="text-muted text-center">No songs in this album.</p>';
        }

        const modalElement = document.getElementById('albumModal');
        const modal = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
        modal.show();

        // Reset repeat mode and button UI for new album
        repeatMode = 'none';
        const repeatBtnEl = document.getElementById('repeat-btn');
        if (repeatBtnEl) {
          repeatBtnEl.classList.remove('btn-primary');
          repeatBtnEl.classList.add('btn-outline-secondary');
          repeatBtnEl.innerHTML = '<i class="bi bi-repeat"></i>';
          repeatBtnEl.title = 'Repeat';
        }

        setupPlayerControls();

        modalElement.addEventListener('hidden.bs.modal', () => {
          if (currentAudioPlayer) {
            currentAudioPlayer.pause();
            currentAudioPlayer = null;
          }
            currentAlbumSongs = [];
            isPlaying = false;
            currentSongIndex = -1;
            repeatMode = 'none';
            const btn = currentTriggerButton;
          if (btn && document.body.contains(btn)) btn.focus();
        }, { once: true });

      } catch (err) {
        alert('Could not load album details: ' + err.message);
        const modal = bootstrap.Modal.getInstance(document.getElementById('albumModal'));
        if (modal) modal.hide();
      }
    });
  });

  function setupPlayerControls() {
    const playPauseBtn = document.getElementById('play-pause-btn');
    const prevBtn = document.getElementById('prev-btn');
    const nextBtn = document.getElementById('next-btn');
    const shuffleBtn = document.getElementById('shuffle-btn');
    const repeatBtn = document.getElementById('repeat-btn');
    const progressBar = document.getElementById('progress-bar');
    progressBar.style.width=0;
    const currentTimeEl = document.getElementById('current-time');
    const totalTimeEl = document.getElementById('total-time');
    const currentSongTitleEl = document.getElementById('current-song-title');
    const volumeSlider = document.getElementById('volume-slider');

    function getRandomSongIndex() {
      if (currentAlbumSongs.length <= 1) return 0;
      let i;
      do {
        i = Math.floor(Math.random() * currentAlbumSongs.length);
      } while (i === currentSongIndex);
      return i;
    }

    function playSong(songIndex) {
      if (!currentAlbumSongs || currentAlbumSongs.length === 0) return;
      if (currentAudioPlayer) {
        currentAudioPlayer.pause();
        currentAudioPlayer.currentTime = 0;
      }
      currentSongIndex = songIndex;
      const song = currentAlbumSongs[songIndex];

      // Always query fresh DOM elements so this works even when called from a setupPlayerControls() that ran for a previous album
      document.querySelectorAll('#modal-song-list .play-song-btn').forEach((b, idx) => {
        b.innerHTML = idx === songIndex ? '<i class="bi bi-pause-circle"></i>' : '<i class="bi bi-play-circle"></i>';
      });

      currentSongTitleEl.textContent = song.title || 'Unknown';

      currentAudioPlayer = new Audio(`/api/songs/file?path=${encodeURIComponent(song.filePath)}`);
      currentAudioPlayer.volume = currentVolume;

      const rows = document.querySelectorAll('#modal-song-list tbody tr');
      rows.forEach((r, idx) => r.classList.toggle('table-active', idx === songIndex));

      currentAudioPlayer.addEventListener('loadedmetadata', function () {
        totalTimeEl.textContent = formatTime(this.duration);
      });

      currentAudioPlayer.addEventListener('timeupdate', function () {
        const p = (this.currentTime / this.duration) * 100;
        progressBar.style.width = p + '%';
        currentTimeEl.textContent = formatTime(this.currentTime);
      });

      currentAudioPlayer.addEventListener('ended', function () {
        if (repeatMode === 'one') {
          // Repeat current song only
          currentAudioPlayer.currentTime = 0;
          currentAudioPlayer.play();
        } else if (repeatMode === 'all') {
          // Repeat entire album (loop all songs)
          const nextIndex = isShuffle ? getRandomSongIndex() : (currentSongIndex + 1) % currentAlbumSongs.length;
          playSong(nextIndex);
        } else {
          // No repeat: play next, stop after last song
          let nextIndex;
          if (isShuffle) {
            nextIndex = getRandomSongIndex();
          } else {
            nextIndex = currentSongIndex < currentAlbumSongs.length - 1 ? currentSongIndex + 1 : 0;
          }
          if (nextIndex === 0 && currentSongIndex === currentAlbumSongs.length - 1 && !isShuffle) {
            // Reached end of album, stop playing
            isPlaying = false;
            if (playPauseBtn) playPauseBtn.innerHTML = '<i class="bi bi-play-fill"></i>';
            document.querySelectorAll('#modal-song-list .play-song-btn').forEach(b => {
              b.innerHTML = '<i class="bi bi-play-circle"></i>';
            });
          } else {
            playSong(nextIndex);
          }
        }
      });

      currentAudioPlayer.addEventListener('play', function () {
        isPlaying = true;
        if (playPauseBtn) playPauseBtn.innerHTML = '<i class="bi bi-pause-fill"></i>';
      });

      currentAudioPlayer.addEventListener('pause', function () {
        isPlaying = false;
        if (playPauseBtn) playPauseBtn.innerHTML = '<i class="bi bi-play-fill"></i>';
      });

      currentAudioPlayer.play().catch(e => {
        if (e.name !== 'AbortError') {
          console.error('Play failed:', e);
          alert('Could not play audio file');
        }
      });
    }

    function pauseSong() {
      if (currentAudioPlayer) currentAudioPlayer.pause();
    }

    function formatTime(seconds) {
      if (isNaN(seconds)) return '0:00';
      const m = Math.floor(seconds / 60);
      const s = Math.floor(seconds % 60);
      return m + ':' + (s < 10 ? '0' : '') + s;
    }

    // Attach to song buttons inside the modal (these are recreated on every album load -> attach every time)
    const playButtons = document.querySelectorAll('.play-song-btn');
    playButtons.forEach(btn => {
      btn.addEventListener('click', function () {
        const idx = parseInt(this.getAttribute('data-song-index'));
        playSong(idx);
      });
    });

    if (!playerControlsInitialized) {
      playerControlsInitialized = true;

      if (shuffleBtn) {
        shuffleBtn.addEventListener('click', function () {
          isShuffle = !isShuffle;
          this.classList.toggle('btn-primary', isShuffle);
          this.classList.toggle('btn-outline-secondary', !isShuffle);
          this.innerHTML = isShuffle ? '<i class="bi bi-shuffle"></i>' : '<i class="bi bi-shuffle"></i>';
        });
      }

      if (repeatBtn) {
        repeatBtn.addEventListener('click', function () {
          if (repeatMode === 'none') {
            repeatMode = 'all';
            this.classList.add('btn-primary');
            this.classList.remove('btn-outline-secondary');
            this.innerHTML = '<i class="bi bi-repeat"></i>';
            this.title = 'Repeat album';
          } else if (repeatMode === 'all') {
            repeatMode = 'one';
            this.innerHTML = '<i class="bi bi-repeat-1"></i>';
            this.title = 'Repeat current song';
          } else {
            repeatMode = 'none';
            this.classList.remove('btn-primary');
            this.classList.add('btn-outline-secondary');
            this.innerHTML = '<i class="bi bi-repeat"></i>';
            this.title = 'Repeat';
          }
        });
      }

      if (playPauseBtn) {
        playPauseBtn.addEventListener('click', function () {
          if (isPlaying) {
            pauseSong();
          } else {
            if (currentAudioPlayer) {
              currentAudioPlayer.play();
              isPlaying = true;
              this.innerHTML = '<i class="bi bi-pause-fill"></i>';
            } else if (currentAlbumSongs.length > 0) {
              playSong(0);
            }
          }
        });
      }

      if (prevBtn) {
        prevBtn.addEventListener('click', function () {
          const newIndex = isShuffle ? getRandomSongIndex() : (currentSongIndex > 0 ? currentSongIndex - 1 : currentAlbumSongs.length - 1);
          playSong(newIndex);
        });
      }

      if (nextBtn) {
        nextBtn.addEventListener('click', function () {
          const newIndex = isShuffle ? getRandomSongIndex() : (currentSongIndex < currentAlbumSongs.length - 1 ? currentSongIndex + 1 : 0);
          playSong(newIndex);
        });
      }

      const progressContainer = progressBar.parentElement;
      progressContainer.addEventListener('click', function (e) {
        if (currentAudioPlayer && currentAudioPlayer.duration) {
          const rect = this.getBoundingClientRect();
          const clickX = e.clientX - rect.left;
          const percentage = clickX / rect.width;
          currentAudioPlayer.currentTime = percentage * currentAudioPlayer.duration;
        }
      });

      if (volumeSlider) {
        volumeSlider.addEventListener('input', function () {
          currentVolume = this.value / 100;
          if (currentAudioPlayer) currentAudioPlayer.volume = currentVolume;
        });
      }

      // Ensure initial visual state of repeat button (first time controls are initialized)
      if (repeatBtn) {
        repeatBtn.classList.remove('btn-primary');
        repeatBtn.classList.add('btn-outline-secondary');
        repeatBtn.innerHTML = '<i class="bi bi-repeat"></i>';
        repeatBtn.title = 'Repeat';
      }
    }
  }
}

initAlbums();
