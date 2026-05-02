document.getElementById('sizeSelect').addEventListener('change', function() {
    const size = this.value;
    const url = new URL(window.location);
    url.searchParams.set('size', size);
    url.searchParams.set('page', '0'); // Reset to first page
    // Keep other params
    window.location.href = url.toString();
});

function addSongToPlaylist(button) {
    const songId = button.getAttribute('data-song-id');

    // First, fetch available playlists
    fetch('/api/playlists')
        .then(response => response.json())
        .then(data => {
            if (data.success && data.data.length > 0) {
                showPlaylistSelectionModal(songId, data.data);
            } else {
                alert('No playlists available. Create a playlist first.');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error loading playlists');
        });
}

function showPlaylistSelectionModal(songId, playlists) {
    // Create modal HTML
    const modalHtml = `
        <div class="modal fade" id="selectPlaylistModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">Add Song to Playlist</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <p>Select a playlist to add this song to:</p>
                        <div class="list-group">
                            ${playlists.map(playlist => `
                                <button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
                                        data-playlist-id="${playlist.id}"
                                        onclick="selectPlaylist(${playlist.id}, ${songId}, this)">
                                    <div>
                                        <div class="fw-bold">${playlist.name}</div>
                                        <small class="text-muted">${playlist.songCount} songs</small>
                                    </div>
                                    <i class="bi bi-plus-circle text-success"></i>
                                </button>
                            `).join('')}
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    // Remove existing modal if present
    const existingModal = document.getElementById('selectPlaylistModal');
    if (existingModal) {
        existingModal.remove();
    }

    // Add modal to body
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('selectPlaylistModal'));
    modal.show();
}

function selectPlaylist(playlistId, songId, button) {
    button.disabled = true;
    button.innerHTML = `
        <div>
            <div class="fw-bold">${button.querySelector('.fw-bold').textContent}</div>
            <small class="text-muted">Adding...</small>
        </div>
        <div class="spinner-border spinner-border-sm text-success" role="status"></div>
    `;

    fetch(`/api/playlists/${playlistId}/songs/${songId}`, {
        method: 'POST'
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                button.innerHTML = `
                <div>
                    <div class="fw-bold">${button.querySelector('.fw-bold').textContent}</div>
                    <small class="text-success">Added successfully!</small>
                </div>
                <i class="bi bi-check-circle text-success"></i>
            `;
                setTimeout(() => {
                    const modal = bootstrap.Modal.getInstance(document.getElementById('selectPlaylistModal'));
                    modal.hide();
                }, 1500);
            } else {
                alert('Error adding song to playlist: ' + (data.message || 'Unknown error'));
                button.disabled = false;
                button.innerHTML = `
                <div>
                    <div class="fw-bold">${button.querySelector('.fw-bold').textContent}</div>
                    <small class="text-muted">${button.querySelector('.text-muted').textContent}</small>
                </div>
                <i class="bi bi-plus-circle text-success"></i>
            `;
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error adding song to playlist');
            button.disabled = false;
            button.innerHTML = `
            <div>
                <div class="fw-bold">${button.querySelector('.fw-bold').textContent}</div>
                <small class="text-muted">${button.querySelector('.text-muted').textContent}</small>
            </div>
            <i class="bi bi-plus-circle text-success"></i>
        `;
        });
}