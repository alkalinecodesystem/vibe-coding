document.addEventListener('DOMContentLoaded', function() {
    let currentAudioPlayer = null;
    let currentSongIndex = -1;
    let currentAlbumSongs = [];
    let isPlaying = false;
    let isShuffle = false;
    let isRepeat = false;
    let currentVolume = 0.5;

    const playAlbumButtons = document.querySelectorAll('.play-album-btn');
    console.log('Found', playAlbumButtons.length, 'play album buttons');
    console.log('Play album buttons:', playAlbumButtons);

    let currentTriggerButton = null;
    let currentArtistTriggerButton = null;

    playAlbumButtons.forEach(button => {
        console.log('Adding event listener to button:', button);
        button.addEventListener('click', async function(event) {
            event.preventDefault();
            console.log('Play album button clicked');
            console.log('Button element:', this);
            console.log('Button attributes:', this.attributes);

            // Store reference to the trigger button for focus management
            currentTriggerButton = this;

            const albumId = this.getAttribute('data-album-id');
            const albumTitle = this.getAttribute('data-album-title');
            const albumArtist = this.getAttribute('data-album-artist');
            console.log('Album ID:', albumId, 'Title:', albumTitle, 'Artist:', albumArtist);
            console.log('Album ID type:', typeof albumId, 'value:', albumId);

            if (!albumId) {
                console.error('No album ID found! Button attributes:', this.attributes);
                alert('Error: No album ID found. Please check that the album data is loading correctly.');
                return;
            }

            // Set modal title and artist
            document.getElementById('modal-album-title').textContent = albumTitle;
            document.getElementById('modal-album-artist').textContent = albumArtist;

            try {
                // Fetch album details including songs
                console.log('Fetching album data from:', `/api/albums/${albumId}`);
                const response = await fetch(`/api/albums/${albumId}`);
                console.log('Fetch response status:', response.status);
                console.log('Fetch response ok:', response.ok);

                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('Error response:', errorText);
                    throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
                }

                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const responseJson = await response.json();
                const data = responseJson.data; // 👈 IMPORTANTE

                currentAlbumSongs = data.songs || [];
                console.log('Received album data:', data);

                // Set cover image if available
                const coverImg = document.getElementById('modal-album-cover');
                if (data.hasCover && data.coverContentType && data.coverImageBase64) {
                    coverImg.src = 'data:' + data.coverContentType + ';base64,' + data.coverImageBase64;
                    coverImg.style.display = 'center';
                } else {
                    coverImg.style.display = 'none';
                }

                // Store songs data for player controls
                currentAlbumSongs = data.songs || [];
                // Sort by track number (nulls last)
                currentAlbumSongs.sort((a, b) => {
                    const trackA = a.trackNumber != null ? a.trackNumber : Number.MAX_VALUE;
                    const trackB = b.trackNumber != null ? b.trackNumber : Number.MAX_VALUE;
                    return trackA - trackB;
                });
                currentSongIndex = -1;

                // Build song list
                const songListContainer = document.getElementById('modal-song-list');
                songListContainer.innerHTML = ''; // clear

                if (currentAlbumSongs.length > 0) {
                    // Show audio controls
                    document.getElementById('audio-controls').style.display = 'block';

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
                    currentAlbumSongs.forEach((song, index) => {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                        <td>${index + 1}</td>
                        <td><strong>${song.title || ''}</strong></td>
                        <td>${song.trackNumber ?? '-'}</td>
                        <td>${song.formattedDuration ?? ''}</td>
                        <td>${song.genre ? `<span class="badge bg-secondary">${song.genre}</span>` : '-'}</td>
                        <td>${song.originalArtist ?? (data.originalArtist || '')}</td>
                        <td>
                            <button class="btn btn-sm btn-outline-primary play-song-btn" data-song-index="${index}">
                                <i class="bi bi-play-circle"></i>
                            </button>
                        </td>
                    `;
                        tbody.appendChild(tr);
                    });
                    songListContainer.appendChild(table);
                } else {
                    document.getElementById('audio-controls').style.display = 'none';
                    songListContainer.innerHTML = '<p class="text-muted text-center">No songs in this album.</p>';
                }

                // Show modal
                console.log('About to show modal');
                const modalElement = document.getElementById('albumModal');
                console.log('Modal element found:', modalElement);
                if (modalElement) {
                    // Use Bootstrap's data attributes to show modal instead of manual instance
                    const modal = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
                    modal.show();
                    console.log('Modal shown successfully');

                    // Stop playback when modal is closed
                    modalElement.addEventListener('hidden.bs.modal', function() {
                        if (currentAudioPlayer) {
                            currentAudioPlayer.pause();
                            currentAudioPlayer.currentTime = 0;
                            currentAudioPlayer = null;
                        }
                        isPlaying = false;
                        currentSongIndex = -1;
                        currentAlbumSongs = [];

                        // Dispose modal instance and move focus back to trigger button after modal is fully hidden
                        const modalInstance = bootstrap.Modal.getInstance(modalElement);
                        if (modalInstance) {
                            modalInstance.dispose();
                        }

                        setTimeout(() => {
                            if (currentTriggerButton && document.body.contains(currentTriggerButton)) {
                                currentTriggerButton.focus();
                            }
                        }, 100);
                    });

                    // Setup player controls after modal is shown
                    setTimeout(() => {
                        setupPlayerControls();
                    }, 100);
                } else {
                    console.error('Modal element not found!');
                    alert('Modal not found. Please refresh the page.');
                }

            } catch (err) {
                console.error('Error fetching album details:', err);
                console.error('Error stack:', err.stack);
                alert('Could not load album details: ' + err.message);
            }
        });
    });

    // Handle view artist buttons
    const viewArtistButtons = document.querySelectorAll('.view-artist-btn');
    viewArtistButtons.forEach(button => {
        button.addEventListener('click', async function(event) {
            event.preventDefault();

            currentArtistTriggerButton = this;

            const artistId = this.getAttribute('data-artist-id');
            const artistName = this.getAttribute('data-artist-name');

            if (!artistId) {
                alert('Error: No artist ID found.');
                return;
            }

            try {
                // Fetch artist details
                const response = await fetch(`/api/artists/${artistId}`);
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const responseJson = await response.json();
                const artist = responseJson.data;

                // Populate modal
                const artistInfoDiv = document.getElementById('artist-info');
                const biographyText = artist.biography || '';
                artistInfoDiv.innerHTML = `
                    <div class="row">
                        <div class="col-md-12">
                            <h4>${artist.name}</h4>
                            <div class="mb-3">
                                <label for="artist-biography" class="form-label">Biography</label>
                                <textarea class="form-control" id="artist-biography" rows="6" maxlength="1000">${biographyText}</textarea>
                                <div class="form-text">
                                    <small id="biography-char-count" class="text-muted">${biographyText.length}/1000 characters</small>
                                </div>
                            </div>
                            <div class="mb-3">
                                <h5>Albums</h5>
                                <ul class="list-group">
                                    ${artist.albums && artist.albums.length > 0 ? artist.albums.map(album => `
                                        <li class="list-group-item">
                                            <strong>${album.title}</strong> ${album.releaseYear ? `(${album.releaseYear})` : ''}
                                        </li>
                                    `).join('') : '<li class="list-group-item text-muted">No albums found</li>'}
                                </ul>
                            </div>
                            <div class="d-flex justify-content-end">
                                <button type="button" class="btn btn-secondary me-2" data-bs-dismiss="modal">Close</button>
                                <button type="button" class="btn btn-primary" id="save-artist-btn" data-artist-id="${artistId}">Save Changes</button>
                            </div>
                        </div>
                    </div>
                `;

                // Setup biography textarea character counter
                const biographyTextarea = document.getElementById('artist-biography');
                const charCountElement = document.getElementById('biography-char-count');

                biographyTextarea.addEventListener('input', function() {
                    const currentLength = this.value.length;
                    charCountElement.textContent = `${currentLength}/1000 characters`;

                    // Change color when approaching limit
                    if (currentLength > 900) {
                        charCountElement.className = 'text-warning';
                    } else if (currentLength > 950) {
                        charCountElement.className = 'text-danger';
                    } else {
                        charCountElement.className = 'text-muted';
                    }
                });

                // Setup save button
                document.getElementById('save-artist-btn').addEventListener('click', async function() {
                    const biography = document.getElementById('artist-biography').value;
                    try {
                        const updateResponse = await fetch(`/api/artists/${artistId}`, {
                            method: 'PUT',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({
                                name: artist.name,
                                biography: biography
                            })
                        });

                        if (!updateResponse.ok) {
                            throw new Error(`HTTP error! status: ${updateResponse.status}`);
                        }

                        // Close modal
                        const modal = bootstrap.Modal.getInstance(document.getElementById('artistModal'));
                        modal.hide();
                    } catch (err) {
                        alert('Error updating artist: ' + err.message);
                    }
                });

                // Show modal
                const modalElement = document.getElementById('artistModal');
                const modal = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
                modal.show();

                // Handle modal close
                modalElement.addEventListener('hidden.bs.modal', function() {
                    const modalInstance = bootstrap.Modal.getInstance(modalElement);
                    if (modalInstance) {
                        modalInstance.dispose();
                    }
                    setTimeout(() => {
                        if (currentArtistTriggerButton && document.body.contains(currentArtistTriggerButton)) {
                            currentArtistTriggerButton.focus();
                        }
                    }, 100);
                });

            } catch (err) {
                alert('Could not load artist details: ' + err.message);
            }
        });
    });

    function setupPlayerControls() {
        const playButtons = document.querySelectorAll('.play-song-btn');
        const playPauseBtn = document.getElementById('play-pause-btn');
        const prevBtn = document.getElementById('prev-btn');
        const nextBtn = document.getElementById('next-btn');
        const shuffleBtn = document.getElementById('shuffle-btn');
        const repeatBtn = document.getElementById('repeat-btn');
        const progressBar = document.getElementById('progress-bar');
        const currentTimeEl = document.getElementById('current-time');
        const totalTimeEl = document.getElementById('total-time');
        const currentSongTitleEl = document.getElementById('current-song-title');
        const volumeSlider = document.getElementById('volume-slider');

        // Shuffle toggle
        if (shuffleBtn) {
            shuffleBtn.addEventListener('click', function() {
                isShuffle = !isShuffle;
                this.classList.toggle('btn-primary', isShuffle);
                this.classList.toggle('btn-outline-secondary', !isShuffle);
                this.innerHTML = isShuffle ? '<i class="bi bi-shuffle"></i>' : '<i class="bi bi-shuffle"></i>';
            });
        }

        // Repeat toggle
        if (repeatBtn) {
            repeatBtn.addEventListener('click', function() {
                isRepeat = !isRepeat;
                this.classList.toggle('btn-primary', isRepeat);
                this.classList.toggle('btn-outline-secondary', !isRepeat);
                this.innerHTML = isRepeat ? '<i class="bi bi-repeat"></i>' : '<i class="bi bi-repeat"></i>';
            });
        }

        // Play specific song
        playButtons.forEach(btn => {
            btn.addEventListener('click', function() {
                const songIndex = parseInt(this.getAttribute('data-song-index'));
                playSong(songIndex);
            });
        });

        // Play/Pause toggle
        if (playPauseBtn) {
            playPauseBtn.addEventListener('click', function() {
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

        // Previous song
        if (prevBtn) {
            prevBtn.addEventListener('click', function() {
                let newIndex;
                if (isShuffle) {
                    newIndex = getRandomSongIndex();
                } else {
                    newIndex = currentSongIndex > 0 ? currentSongIndex - 1 : currentAlbumSongs.length - 1;
                }
                playSong(newIndex);
            });
        }

        // Next song
        if (nextBtn) {
            nextBtn.addEventListener('click', function() {
                let newIndex;
                if (isShuffle) {
                    newIndex = getRandomSongIndex();
                } else {
                    newIndex = currentSongIndex < currentAlbumSongs.length - 1 ? currentSongIndex + 1 : 0;
                }
                playSong(newIndex);
            });
        }

        function getRandomSongIndex() {
            if (currentAlbumSongs.length <= 1) return 0;
            let newIndex;
            do {
                newIndex = Math.floor(Math.random() * currentAlbumSongs.length);
            } while (newIndex === currentSongIndex);
            return newIndex;
        }

        function playSong(songIndex) {
            // Stop current audio
            if (currentAudioPlayer) {
                currentAudioPlayer.pause();
                currentAudioPlayer.currentTime = 0;
            }

            currentSongIndex = songIndex;
            const song = currentAlbumSongs[songIndex];

            // Update UI
            playButtons.forEach((btn, idx) => {
                btn.innerHTML = idx === songIndex ? '<i class="bi bi-pause-circle"></i>' : '<i class="bi bi-play-circle"></i>';
            });

            currentSongTitleEl.textContent = song.title || 'Unknown';

            // Create new audio player
            currentAudioPlayer = new Audio(`/api/songs/file?path=${encodeURIComponent(song.filePath)}`);
            currentAudioPlayer.volume = currentVolume;

            const rows = document.querySelectorAll('#modal-song-list tbody tr');

            rows.forEach((row, idx) => {
                row.classList.toggle('table-active', idx === songIndex);
            });

            // Set up event listeners
            currentAudioPlayer.addEventListener('loadedmetadata', function() {
                totalTimeEl.textContent = formatTime(this.duration);
            });

            currentAudioPlayer.addEventListener('timeupdate', function() {
                const progress = (this.currentTime / this.duration) * 100;
                progressBar.style.width = progress + '%';
                currentTimeEl.textContent = formatTime(this.currentTime);
            });

            currentAudioPlayer.addEventListener('ended', function() {
                if (isRepeat) {
                    // Repeat current song
                    currentAudioPlayer.currentTime = 0;
                    currentAudioPlayer.play();
                } else {
                    // Auto-play next song
                    let nextIndex;
                    if (isShuffle) {
                        nextIndex = getRandomSongIndex();
                    } else {
                        nextIndex = currentSongIndex < currentAlbumSongs.length - 1 ? currentSongIndex + 1 : 0;
                    }
                    if (nextIndex === 0 && currentSongIndex === currentAlbumSongs.length - 1 && !isShuffle) {
                        // Reached end of album, stop playing
                        isPlaying = false;
                        playPauseBtn.innerHTML = '<i class="bi bi-play-fill"></i>';
                        playButtons.forEach(btn => btn.innerHTML = '<i class="bi bi-play-circle"></i>');
                    } else {
                        playSong(nextIndex);
                    }
                }
            });

            currentAudioPlayer.addEventListener('play', function() {
                isPlaying = true;
                playPauseBtn.innerHTML = '<i class="bi bi-pause-fill"></i>';
            });

            currentAudioPlayer.addEventListener('pause', function() {
                isPlaying = false;
                playPauseBtn.innerHTML = '<i class="bi bi-play-fill"></i>';
            });
            // Start playing
            currentAudioPlayer.play().catch(e => {
                console.error('Play failed:', e);
                alert('Could not play audio file');
            });
        }

        function pauseSong() {
            if (currentAudioPlayer) {
                currentAudioPlayer.pause();
            }
        }

        function formatTime(seconds) {
            if (isNaN(seconds)) return '0:00';
            const mins = Math.floor(seconds / 60);
            const secs = Math.floor(seconds % 60);
            return mins + ':' + (secs < 10 ? '0' : '') + secs;
        }

        // Click on progress bar to seek
        const progressContainer = progressBar.parentElement;
        progressContainer.addEventListener('click', function(e) {
            if (currentAudioPlayer && currentAudioPlayer.duration) {
                const rect = this.getBoundingClientRect();
                const clickX = e.clientX - rect.left;
                const percentage = clickX / rect.width;
                currentAudioPlayer.currentTime = percentage * currentAudioPlayer.duration;
            }
        });

        // Volume slider
        volumeSlider.addEventListener('input', function() {
            currentVolume = this.value / 100;
            if (currentAudioPlayer) {
                currentAudioPlayer.volume = currentVolume;
            }
        });
    }
});