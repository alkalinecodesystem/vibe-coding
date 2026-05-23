// Playlist detail page JavaScript
let currentPlaylistId;
let playlistSongs = [];
let playlistCurrentAudioPlayer = null;
let playlistCurrentSongIndex = -1;
let playlistIsPlaying = false;
let playlistIsShuffle = false;
let playlistRepeatMode = 'none'; // 'none' | 'all' | 'one'
let playlistCurrentVolume = 0.5;
let deleteSongPlaylistId = null;
let deleteSongId = null;
let playlistPlayerControlsInitialized = false;
let selectedSongs = new Set(); // for bulk add UI (currently unused in template but prevents ReferenceError)

// Pagination variables for add songs modal
let currentPage = 0;
let pageSize = 10;
let totalPages = 0;
let currentSearchQuery = '';

function initPlaylistDetail() {
    // Get playlist ID from URL or data attribute
    const playlistCard = document.querySelector('[data-playlist-id]');
    if (playlistCard) {
        currentPlaylistId = playlistCard.getAttribute('data-playlist-id');
    } else {
        // Try to extract from URL
        const urlParts = window.location.pathname.split('/');
        const playlistIndex = urlParts.indexOf('playlists');
        if (playlistIndex !== -1 && urlParts.length > playlistIndex + 1) {
            currentPlaylistId = urlParts[playlistIndex + 1];
        }
    }

    // Setup song search functionality (guard to support re-init)
    const songSearch = document.getElementById('songSearch');
    if (songSearch && !songSearch.dataset.playlistSearchListener) {
        songSearch.dataset.playlistSearchListener = 'true';
        songSearch.addEventListener('input', debounce(searchSongs, 300));
    }

    // Setup size selector (guarded)
    const sizeSelect = document.getElementById('sizeSelect');
    if (sizeSelect && !sizeSelect.dataset.playlistSizeListener) {
        sizeSelect.dataset.playlistSizeListener = 'true';
        sizeSelect.addEventListener('change', function() {
            pageSize = parseInt(this.value);
            loadSongsForAdding(currentSearchQuery, 0);
        });
    }

    // Load initial songs when modal is shown (attach only once)
    const addSongsModal = document.getElementById('addSongsModal');
    if (addSongsModal && !addSongsModal.dataset.playlistModalListeners) {
        addSongsModal.dataset.playlistModalListeners = 'true';
        addSongsModal.addEventListener('shown.bs.modal', function() {
            loadSongsForAdding('', 0);
        });

        // Reload playlist when modal is closed to show added songs
        addSongsModal.addEventListener('hidden.bs.modal', function() {
            location.reload();
        });
    }

    // Setup play playlist modal (attach only once)
    const playPlaylistModal = document.getElementById('playPlaylistModal');
    if (playPlaylistModal && !playPlaylistModal.dataset.playlistModalListeners) {
        playPlaylistModal.dataset.playlistModalListeners = 'true';
        
        playPlaylistModal.addEventListener('shown.bs.modal', function() {
            loadPlaylistForPlaying();
        });

        playPlaylistModal.addEventListener('hidden.bs.modal', function() {
            // Stop playback when modal is closed
            if (playlistCurrentAudioPlayer) {
                playlistCurrentAudioPlayer.pause();
                playlistCurrentAudioPlayer.currentTime = 0;
                playlistCurrentAudioPlayer = null;
            }
            playlistIsPlaying = false;
            playlistCurrentSongIndex = -1;
            playlistSongs = [];
            playlistRepeatMode = 'none';
        });
    }

    // Setup delete playlist modal (attach only once)
    const deletePlaylistModal = document.getElementById('deletePlaylistModal');
    if (deletePlaylistModal && !deletePlaylistModal.dataset.playlistModalListeners) {
        deletePlaylistModal.dataset.playlistModalListeners = 'true';
        deletePlaylistModal.addEventListener('show.bs.modal', function(event) {
            const button = event.relatedTarget;
            const playlistName = button.getAttribute('data-playlist-name');
            const nameEl = document.getElementById('delete-playlist-name');
            if (nameEl) nameEl.textContent = playlistName || '';
        });
    }
}

initPlaylistDetail();

// savePlaylist function is now handled by the modal - this function is kept for backward compatibility but should not be used

function setDeleteSongData(button) {
    deleteSongPlaylistId = button.getAttribute('data-playlist-id') || currentPlaylistId;
    deleteSongId = button.getAttribute('data-song-id');
    const songTitle = button.getAttribute('data-song-title');

    document.getElementById('delete-song-title').textContent = songTitle;
}

function confirmRemoveSong() {
    if (!deleteSongPlaylistId || !deleteSongId) return;

    const confirmBtn = document.getElementById('confirmDeleteSongBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<i class="bi bi-hourglass"></i> Removing...';

    fetch(`/api/playlists/${deleteSongPlaylistId}/songs/${deleteSongId}`, {
        method: 'DELETE'
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // Close modal and reload page
                const modal = bootstrap.Modal.getInstance(document.getElementById('deleteSongModal'));
                modal.hide();
                location.reload();
            } else {
                alert('Error removing song: ' + (data.message || 'Unknown error'));
                confirmBtn.disabled = false;
                confirmBtn.innerHTML = '<i class="bi bi-trash"></i> Remove Song';
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error removing song');
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="bi bi-trash"></i> Remove Song';
        });
}

function confirmSavePlaylist(button) {
    const playlistId = button.getAttribute('data-playlist-id') || currentPlaylistId;

    const confirmBtn = document.getElementById('confirmSavePlaylistBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<i class="bi bi-hourglass"></i> Saving...';

    fetch(`/api/playlists/${playlistId}/save`, {
        method: 'POST'
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // Close modal and show success message
                const modal = bootstrap.Modal.getInstance(document.getElementById('savePlaylistModal'));
                modal.hide();

                // Show success message temporarily
                showSuccessMessage('Playlist saved successfully!');
            } else {
                alert('Error saving playlist: ' + (data.message || 'Unknown error'));
                confirmBtn.disabled = false;
                confirmBtn.innerHTML = '<i class="bi bi-download"></i> Save Playlist';
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error saving playlist');
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="bi bi-download"></i> Save Playlist';
        });
}

function showSuccessMessage(message) {
    // Create and show a success toast/alert
    const alertDiv = document.createElement('div');
    alertDiv.className = 'alert alert-success alert-dismissible fade show position-fixed';
    alertDiv.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
    alertDiv.innerHTML = `
        <i class="bi bi-check-circle-fill me-2"></i>${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;

    document.body.appendChild(alertDiv);

    // Auto remove after 3 seconds
    setTimeout(() => {
        if (alertDiv.parentNode) {
            alertDiv.remove();
        }
    }, 3000);
}

function searchSongs() {
    const query = document.getElementById('songSearch').value.trim();
    loadSongsForAdding(query, 0);
}

function loadSongsForAdding(searchQuery = '', page = 0) {
    currentPage = page;
    currentSearchQuery = searchQuery;
    const songsList = document.getElementById('songsList');
    songsList.innerHTML = '<div class="text-center py-3"><div class="spinner-border spinner-border-sm" role="status"></div> Loading songs...</div>';

    let url = `/api/songs?page=${page}&size=${pageSize}`;
    if (searchQuery) {
        url += `&q=${encodeURIComponent(searchQuery)}&type=all`;
    }

    fetch(url)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                totalPages = data.data.totalPages;
                renderSongsList(data.data.content, songsList);
                renderPagination();
            } else {
                songsList.innerHTML = '<div class="text-center py-3 text-muted">Error loading songs</div>';
            }
        })
        .catch(error => {
            console.error('Error:', error);
            songsList.innerHTML = '<div class="text-center py-3 text-muted">Error loading songs</div>';
        });
}

function renderSongsList(songs, container) {
    if (!songs || songs.length === 0) {
        container.innerHTML = '<div class="text-center py-3 text-muted">No songs found</div>';
        return;
    }

    container.innerHTML = '';

    songs.forEach(song => {
        const songItem = document.createElement('div');
        songItem.className = 'list-group-item list-group-item-action d-flex justify-content-between align-items-center';
        songItem.innerHTML = `
            <div class="flex-grow-1">
                <div class="fw-bold">${song.title}</div>
                <small class="text-muted">
                    ${song.originalArtist || 'Unknown Artist'} •
                    ${song.album ? song.album.title : 'Unknown Album'}
                </small>
            </div>
            <button class="btn btn-sm btn-outline-success" data-song-id="${song.id}" onclick="addSongToPlaylistFromModal(this)">
                <i class="bi bi-plus-circle"></i> Add
            </button>
        `;
        container.appendChild(songItem);
    });
}

function addSongToPlaylistFromModal(button) {
    const songId = button.getAttribute('data-song-id');
    const playlistId = currentPlaylistId;
    const songItem = button.closest('.list-group-item');

    button.disabled = true;
    button.innerHTML = '<i class="bi bi-hourglass"></i> Adding...';

    fetch(`/api/playlists/${playlistId}/songs/${songId}`, {
        method: 'POST'
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                button.innerHTML = '<i class="bi bi-check-circle"></i> Added';
                button.className = 'btn btn-sm btn-success';

                // Remove the song from the list after a short delay
                setTimeout(() => {
                    if (songItem && songItem.parentNode) {
                        songItem.remove();

                        // Check if there are no more songs
                        const songsList = document.getElementById('songsList');
                        const remainingSongs = songsList.querySelectorAll('.list-group-item');
                        if (remainingSongs.length === 0) {
                            songsList.innerHTML = '<div class="text-center py-3 text-muted">No songs available to add</div>';
                        }
                    }
                }, 1000);

                // Update the song count on the page if visible
                updatePlaylistSongCount();

            } else {
                alert('Error adding song: ' + (data.message || 'Unknown error'));
                button.disabled = false;
                button.innerHTML = '<i class="bi bi-plus-circle"></i> Add';
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error adding song');
            button.disabled = false;
            button.innerHTML = '<i class="bi bi-plus-circle"></i> Add';
        });
}

function loadPlaylistForPlaying() {
    // Always load complete playlist data from API to ensure we have filePath
    fetch(`/api/playlists/${currentPlaylistId}`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                setupPlaylistPlayer(data.data);
            } else {
                console.error('Failed to load playlist data:', data.message);
                alert('Error loading playlist for playback');
            }
        })
        .catch(error => {
            console.error('Error loading playlist:', error);
            alert('Error loading playlist for playback');
        });
}

function setupPlaylistPlayer(playlistData) {
    playlistSongs = playlistData.songs || [];
    playlistCurrentSongIndex = -1;

    // Reset repeat mode and UI when loading a (new) playlist
    playlistRepeatMode = 'none';
    const repeatBtnEl = document.getElementById('playlist-repeat-btn');
    if (repeatBtnEl) {
        repeatBtnEl.classList.remove('btn-primary');
        repeatBtnEl.classList.add('btn-outline-secondary');
        repeatBtnEl.innerHTML = '<i class="bi bi-repeat"></i>';
        repeatBtnEl.title = 'Repeat';
    }

    // Build song list in modal
    const songListContainer = document.getElementById('playlist-modal-song-list');
    songListContainer.innerHTML = '';

    if (playlistSongs.length > 0) {
        document.getElementById('playlist-audio-controls').style.display = 'block';

        const table = document.createElement('table');
        table.className = 'table table-hover mb-0';
        table.innerHTML = `
            <thead class="table-light">
                <tr>
                    <th width="60">#</th>
                    <th>Title</th>
                    <th>Track</th>
                    <th>Duration</th>
                    <th>Genre</th>
                    <th>Artist</th>
                    <th>Play</th>
                </tr>
            </thead>
            <tbody>
            </tbody>
        `;
        const tbody = table.querySelector('tbody');
        playlistSongs.forEach((song, index) => {
            const tr = document.createElement('tr');
            tr.setAttribute('data-song-id', song.id);
            tr.setAttribute('data-file-path', song.filePath);
            tr.innerHTML = `
                <td>${index + 1}</td>
                <td><strong>${song.title || ''}</strong></td>
                <td>${song.trackNumber ?? '-'}</td>
                <td>${song.formattedDuration ?? ''}</td>
                <td>${song.genre ? `<span class="badge bg-secondary">${song.genre}</span>` : '-'}</td>
                <td>${song.originalArtist ?? ''}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary playlist-play-song-btn" data-song-index="${index}">
                        <i class="bi bi-play-circle"></i>
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });
        songListContainer.appendChild(table);

        // Setup player controls
        setupPlaylistPlayerControls();
    } else {
        document.getElementById('playlist-audio-controls').style.display = 'none';
        songListContainer.innerHTML = '<p class="text-muted text-center">No songs in this playlist.</p>';
    }
}

function setupPlaylistPlayerControls() {
    const playPauseBtn = document.getElementById('playlist-play-pause-btn');
    const prevBtn = document.getElementById('playlist-prev-btn');
    const nextBtn = document.getElementById('playlist-next-btn');
    const shuffleBtn = document.getElementById('playlist-shuffle-btn');
    const repeatBtn = document.getElementById('playlist-repeat-btn');
    const progressBar = document.getElementById('playlist-progress-bar');
    progressBar.style.width=0;
    const currentTimeEl = document.getElementById('playlist-current-time');
    currentTimeEl.textContent='--:--';
    const totalTimeEl = document.getElementById('playlist-total-time');
    totalTimeEl.textContent='--:--';
    const playlistCurrentSongTitleEl = document.getElementById('playlist-current-song-title');
    playlistCurrentSongTitleEl.textContent='🕱';
    const volumeSlider = document.getElementById('playlist-volume-slider');

    function getRandomPlaylistSongIndex() {
        if (playlistSongs.length <= 1) return 0;
        let newIndex;
        do {
            newIndex = Math.floor(Math.random() * playlistSongs.length);
        } while (newIndex === playlistCurrentSongIndex);
        return newIndex;
    }

    function playPlaylistSong(songIndex) {
        // Stop current audio
        if (playlistCurrentAudioPlayer) {
            playlistCurrentAudioPlayer.pause();
            playlistCurrentAudioPlayer.currentTime = 0;
        }

        playlistCurrentSongIndex = songIndex;
        const song = playlistSongs[songIndex];

        console.log('Playing song:', song);
        console.log('File path:', song.filePath);

        // Update UI - always query fresh to support multiple setups
        document.querySelectorAll('.playlist-play-song-btn').forEach((btn, idx) => {
            btn.innerHTML = idx === songIndex ? '<i class="bi bi-pause-circle"></i>' : '<i class="bi bi-play-circle"></i>';
        });

        playlistCurrentSongTitleEl.textContent = song.title || 'Unknown';

        // Update cover image if available
        updatePlaylistCoverImage(song);

        // Check if filePath exists
        if (!song.filePath) {
            console.error('No filePath for song:', song);
            alert('This song does not have an associated audio file.');
            return;
        }

        // Create new audio player
        const audioUrl = `/api/songs/file?path=${encodeURIComponent(song.filePath)}`;
        console.log('Audio URL:', audioUrl);
        playlistCurrentAudioPlayer = new Audio(audioUrl);
        playlistCurrentAudioPlayer.volume = playlistCurrentVolume;

        const rows = document.querySelectorAll('#playlist-modal-song-list tbody tr');

        rows.forEach((row, idx) => {
            row.classList.toggle('table-active', idx === songIndex);
        });

        const activeRow = rows[songIndex];
        if (activeRow) {
            const container = document.getElementById('playlist-modal-song-list');
            if (container) {
                activeRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }

        // Set up event listeners
        playlistCurrentAudioPlayer.addEventListener('loadedmetadata', function() {
            totalTimeEl.textContent = formatTime(this.duration);
        });

        playlistCurrentAudioPlayer.addEventListener('timeupdate', function() {
            const progress = (this.currentTime / this.duration) * 100;
            progressBar.style.width = progress + '%';
            currentTimeEl.textContent = formatTime(this.currentTime);
        });

        playlistCurrentAudioPlayer.addEventListener('ended', function() {
            if (playlistRepeatMode === 'one') {
                // Repeat current song only
                playlistCurrentAudioPlayer.currentTime = 0;
                playlistCurrentAudioPlayer.play();
            } else if (playlistRepeatMode === 'all') {
                // Repeat entire playlist (loop all songs)
                let nextIndex;
                if (playlistIsShuffle) {
                    nextIndex = getRandomPlaylistSongIndex();
                } else {
                    nextIndex = (playlistCurrentSongIndex + 1) % playlistSongs.length;
                }
                playPlaylistSong(nextIndex);
            } else {
                // No repeat: Auto-play next song (stop after last song)
                let nextIndex;
                if (playlistIsShuffle) {
                    nextIndex = getRandomPlaylistSongIndex();
                } else {
                    nextIndex = playlistCurrentSongIndex < playlistSongs.length - 1 ? playlistCurrentSongIndex + 1 : 0;
                }
                if (nextIndex === 0 && playlistCurrentSongIndex === playlistSongs.length - 1 && !playlistIsShuffle) {
                    // Reached end of playlist, stop playing
                    playlistIsPlaying = false;
                    playPauseBtn.innerHTML = '<i class="bi bi-play-fill"></i>';
                    document.querySelectorAll('.playlist-play-song-btn').forEach(btn => btn.innerHTML = '<i class="bi bi-play-circle"></i>');
                } else {
                    playPlaylistSong(nextIndex);
                }
            }
        });

        playlistCurrentAudioPlayer.addEventListener('play', function() {
            playlistIsPlaying = true;
            playPauseBtn.innerHTML = '<i class="bi bi-pause-fill"></i>';
        });

        playlistCurrentAudioPlayer.addEventListener('pause', function() {
            playlistIsPlaying = false;
            playPauseBtn.innerHTML = '<i class="bi bi-play-fill"></i>';
        });

        playlistCurrentAudioPlayer.addEventListener('error', function(e) {
            console.error('Audio error:', e);
            alert('Error loading audio file. The file may not exist or be inaccessible.');
            // Reset play button state
            document.querySelectorAll('.playlist-play-song-btn').forEach(btn => btn.innerHTML = '<i class="bi bi-play-circle"></i>');
            playPauseBtn.innerHTML = '<i class="bi bi-play-fill"></i>';
            playlistIsPlaying = false;
        });

        // Start playing
        playlistCurrentAudioPlayer.play().catch(e => {
            console.error('Play failed:', e);
            alert('Could not play audio file. Please check that the file exists and is accessible.');
            // Reset play button state
            document.querySelectorAll('.playlist-play-song-btn').forEach(btn => btn.innerHTML = '<i class="bi bi-play-circle"></i>');
            playPauseBtn.innerHTML = '<i class="bi bi-play-fill"></i>';
            playlistIsPlaying = false;
        });
    }

    function pausePlaylistSong() {
        if (playlistCurrentAudioPlayer) {
            playlistCurrentAudioPlayer.pause();
        }
    }

    function updatePlaylistCoverImage(song) {
        const coverContainer = document.getElementById('playlist-cover-container');

        // Clear current content
        coverContainer.innerHTML = '';

        // Check if song has album with cover
        if (song.album && song.album.id) {
            // Fetch album cover dynamically
            fetch(`/api/albums/${song.album.id}/cover`)
                .then(response => {
                    if (response.ok) {
                        return response.blob();
                    } else {
                        throw new Error('No cover available');
                    }
                })
                .then(blob => {
                    const img = document.createElement('img');
                    img.src = URL.createObjectURL(blob);
                    img.alt = 'Album Cover';
                    img.className = 'img-fluid rounded shadow-sm';
                    img.style.maxHeight = '200px';
                    coverContainer.appendChild(img);
                })
                .catch(error => {
                    console.log('No cover available for album:', song.album.id);
                    // Show default icon
                    const icon = document.createElement('i');
                    icon.className = 'bi bi-music-note-beamed fs-1 text-primary';
                    coverContainer.appendChild(icon);
                });
        } else {
            // Show default icon
            const icon = document.createElement('i');
            icon.className = 'bi bi-music-note-beamed fs-1 text-primary';
            coverContainer.appendChild(icon);
        }
    }

    // Always attach to song play buttons (they are recreated each time the playlist is loaded into the modal)
    const playButtons = document.querySelectorAll('.playlist-play-song-btn');
    playButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const songIndex = parseInt(this.getAttribute('data-song-index'));
            playPlaylistSong(songIndex);
        });
    });

    if (!playlistPlayerControlsInitialized) {
        playlistPlayerControlsInitialized = true;

        // Shuffle toggle
        if (shuffleBtn) {
            shuffleBtn.addEventListener('click', function() {
                playlistIsShuffle = !playlistIsShuffle;
                this.classList.toggle('btn-primary', playlistIsShuffle);
                this.classList.toggle('btn-outline-secondary', !playlistIsShuffle);
            });
        }

        // Repeat mode cycle: none → all (playlist loop) → one (current song) → none
        if (repeatBtn) {
            repeatBtn.addEventListener('click', function() {
                if (playlistRepeatMode === 'none') {
                    playlistRepeatMode = 'all';
                    this.classList.add('btn-primary');
                    this.classList.remove('btn-outline-secondary');
                    this.innerHTML = '<i class="bi bi-repeat"></i>';
                    this.title = 'Repeat playlist';
                } else if (playlistRepeatMode === 'all') {
                    playlistRepeatMode = 'one';
                    this.innerHTML = '<i class="bi bi-repeat-1"></i>';
                    this.title = 'Repeat current song';
                } else {
                    playlistRepeatMode = 'none';
                    this.classList.remove('btn-primary');
                    this.classList.add('btn-outline-secondary');
                    this.innerHTML = '<i class="bi bi-repeat"></i>';
                    this.title = 'Repeat';
                }
            });
        }

        // Play/Pause toggle
        if (playPauseBtn) {
            playPauseBtn.addEventListener('click', function() {
                if (playlistIsPlaying) {
                    pausePlaylistSong();
                } else {
                    if (playlistCurrentAudioPlayer) {
                        playlistCurrentAudioPlayer.play();
                        playlistIsPlaying = true;
                        this.innerHTML = '<i class="bi bi-pause-fill"></i>';
                    } else if (playlistSongs.length > 0) {
                        playPlaylistSong(0);
                    }
                }
            });
        }

        // Previous song
        if (prevBtn) {
            prevBtn.addEventListener('click', function() {
                let newIndex;
                if (playlistIsShuffle) {
                    newIndex = getRandomPlaylistSongIndex();
                } else {
                    newIndex = playlistCurrentSongIndex > 0 ? playlistCurrentSongIndex - 1 : playlistSongs.length - 1;
                }
                playPlaylistSong(newIndex);
            });
        }

        // Next song
        if (nextBtn) {
            nextBtn.addEventListener('click', function() {
                let newIndex;
                if (playlistIsShuffle) {
                    newIndex = getRandomPlaylistSongIndex();
                } else {
                    newIndex = playlistCurrentSongIndex < playlistSongs.length - 1 ? playlistCurrentSongIndex + 1 : 0;
                }
                playPlaylistSong(newIndex);
            });
        }

        // Click on progress bar to seek
        const progressContainer = progressBar ? progressBar.parentElement : null;
        if (progressContainer) {
            progressContainer.addEventListener('click', function(e) {
                if (playlistCurrentAudioPlayer && playlistCurrentAudioPlayer.duration) {
                    const rect = this.getBoundingClientRect();
                    const clickX = e.clientX - rect.left;
                    const percentage = clickX / rect.width;
                    playlistCurrentAudioPlayer.currentTime = percentage * playlistCurrentAudioPlayer.duration;
                }
            });
        }

        // Volume slider
        if (volumeSlider) {
            volumeSlider.addEventListener('input', function() {
                playlistCurrentVolume = this.value / 100;
                if (playlistCurrentAudioPlayer) {
                    playlistCurrentAudioPlayer.volume = playlistCurrentVolume;
                }
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

function formatTime(seconds) {
    if (isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return mins + ':' + (secs < 10 ? '0' : '') + secs;
}

function updatePlaylistSongCount() {
    // Update song count in the header stats
    const songCountElements = document.querySelectorAll('h3.text-primary');
    songCountElements.forEach(element => {
        const currentCount = parseInt(element.textContent) || 0;
        element.textContent = currentCount + 1;
    });

    // Also update the text that says "X songs"
    const songsTextElements = document.querySelectorAll('p.text-muted');
    songsTextElements.forEach(element => {
        if (element.textContent.includes('songs')) {
            const match = element.textContent.match(/(\d+)\s+songs/);
            if (match) {
                const currentCount = parseInt(match[1]);
                element.textContent = element.textContent.replace(/\d+\s+songs/, (currentCount + 1) + ' songs');
            }
        }
    });
}

// Utility function for debouncing
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

function updateSelectionUI() {
    const selectedCount = selectedSongs.size;
    const selectedSongsCounter = document.getElementById('selectedSongsCounter');
    const selectedCountEl = document.getElementById('selectedCount');
    const addSelectedSongsBtn = document.getElementById('addSelectedSongsBtn');
    const addSelectedCount = document.getElementById('addSelectedCount');
    const clearSelectionBtn = document.getElementById('clearSelectionBtn');

    if (selectedCount > 0) {
        selectedSongsCounter.style.display = 'block';
        selectedCountEl.textContent = selectedCount;
        addSelectedSongsBtn.disabled = false;
        addSelectedCount.textContent = selectedCount;
        clearSelectionBtn.style.display = 'inline-block';
    } else {
        selectedSongsCounter.style.display = 'none';
        addSelectedSongsBtn.disabled = true;
        addSelectedCount.textContent = '0';
        clearSelectionBtn.style.display = 'none';
    }
}

function selectAllSongs() {
    const checkboxes = document.querySelectorAll('.song-checkbox:not(:checked)');
    checkboxes.forEach(checkbox => {
        checkbox.checked = true;
        selectedSongs.add(checkbox.value);
    });
    updateSelectionUI();
}

function clearSelection() {
    const checkboxes = document.querySelectorAll('.song-checkbox:checked');
    checkboxes.forEach(checkbox => {
        checkbox.checked = false;
        selectedSongs.delete(checkbox.value);
    });
    updateSelectionUI();
}

function addSelectedSongs() {
    if (selectedSongs.size === 0) return;

    const playlistId = currentPlaylistId;
    const addSelectedSongsBtn = document.getElementById('addSelectedSongsBtn');

    addSelectedSongsBtn.disabled = true;
    addSelectedSongsBtn.innerHTML = '<i class="bi bi-hourglass"></i> Adding...';

    const promises = Array.from(selectedSongs).map(songId =>
        fetch(`/api/playlists/${playlistId}/songs/${songId}`, {
            method: 'POST'
        }).then(response => response.json())
    );

    Promise.all(promises)
        .then(results => {
            const successCount = results.filter(result => result.success).length;
            const errorCount = results.length - successCount;

            if (successCount > 0) {
                showSuccessMessage(`${successCount} songs added to playlist successfully!`);
                selectedSongs.clear();

                // Reload the page to show updated playlist
                setTimeout(() => {
                    location.reload();
                }, 1500);
            }

            if (errorCount > 0) {
                alert(`${errorCount} songs could not be added. Please try again.`);
            }

            addSelectedSongsBtn.disabled = false;
            addSelectedSongsBtn.innerHTML = '<i class="bi bi-plus-circle"></i> Add Selected (<span id="addSelectedCount">0</span>)';
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error adding songs to playlist');
            addSelectedSongsBtn.disabled = false;
            addSelectedSongsBtn.innerHTML = '<i class="bi bi-plus-circle"></i> Add Selected (<span id="addSelectedCount">0</span>)';
        });
}



function renderPagination() {
    const paginationControls = document.getElementById('paginationControls');
    paginationControls.innerHTML = '<ul class="pagination pagination-sm"></ul>';
    const ul = paginationControls.querySelector('ul');

    // Always render pagination controls, even for single page

    // First button
    const firstLi = document.createElement('li');
    firstLi.className = `page-item ${currentPage === 0 ? 'disabled' : ''}`;
    firstLi.innerHTML = `<a class="page-link" href="#" aria-label="First"><span aria-hidden="true">&laquo;&laquo;</span></a>`;
    firstLi.addEventListener('click', (e) => {
        e.preventDefault();
        if (currentPage > 0) {
            loadSongsForAdding(currentSearchQuery, 0);
        }
    });
    ul.appendChild(firstLi);

    // Previous button
    const prevLi = document.createElement('li');
    prevLi.className = `page-item ${currentPage === 0 ? 'disabled' : ''}`;
    prevLi.innerHTML = `<a class="page-link" href="#" aria-label="Previous"><span aria-hidden="true">&laquo;</span></a>`;
    prevLi.addEventListener('click', (e) => {
        e.preventDefault();
        if (currentPage > 0) {
            loadSongsForAdding(currentSearchQuery, currentPage - 1);
        }
    });
    ul.appendChild(prevLi);

    // Page numbers
    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(totalPages - 1, currentPage + 2);

    for (let i = startPage;i <= endPage;i++) {
        const pageLi = document.createElement('li');
        pageLi.className = `page-item ${i === currentPage ? 'active' : ''}`;
        pageLi.innerHTML = `<a class="page-link" href="#">${i + 1}</a>`;
        pageLi.addEventListener('click', (e) => {
            e.preventDefault();
            loadSongsForAdding(currentSearchQuery, i);
        });
        ul.appendChild(pageLi);
    }

    // Next button
    const nextLi = document.createElement('li');
    nextLi.className = `page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}`;
    nextLi.innerHTML = `<a class="page-link" href="#" aria-label="Next"><span aria-hidden="true">&raquo;</span></a>`;
    nextLi.addEventListener('click', (e) => {
        e.preventDefault();
        if (currentPage < totalPages - 1) {
            loadSongsForAdding(currentSearchQuery, currentPage + 1);
        }
    });
    ul.appendChild(nextLi);

    // Last button
    const lastLi = document.createElement('li');
    lastLi.className = `page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}`;
    lastLi.innerHTML = `<a class="page-link" href="#" aria-label="Last"><span aria-hidden="true">&raquo;&raquo;</span></a>`;
    lastLi.addEventListener('click', (e) => {
        e.preventDefault();
        if (currentPage < totalPages - 1) {
            loadSongsForAdding(currentSearchQuery, totalPages - 1);
        }
    });
    ul.appendChild(lastLi);
}

function confirmDeletePlaylist() {
    if (!currentPlaylistId) return;

    const confirmBtn = document.getElementById('confirmDeletePlaylistBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<i class="bi bi-hourglass"></i> Deleting...';

    fetch(`/api/playlists/${currentPlaylistId}`, {
        method: 'DELETE'
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                window.location.href = '/playlists';
            } else {
                alert('Error deleting playlist: ' + (data.message || 'Unknown error'));
                confirmBtn.disabled = false;
                confirmBtn.innerHTML = '<i class="bi bi-trash"></i> Delete Playlist';
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error deleting playlist');
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="bi bi-trash"></i> Delete Playlist';
        });
}