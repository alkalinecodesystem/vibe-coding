// Playlists page JavaScript
let currentSavePlaylistId = null;
let currentDeletePlaylistId = null;

document.addEventListener('DOMContentLoaded', function() {
    const createPlaylistForm = document.getElementById('createPlaylistForm');

    if (createPlaylistForm) {
        createPlaylistForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const formData = new FormData(this);
            const playlistData = {
                name: formData.get('name'),
                description: formData.get('description')
            };

            fetch('/api/playlists', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(playlistData)
            })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        // Close modal and reload page
                        const modal = bootstrap.Modal.getInstance(document.getElementById('createPlaylistModal'));
                        modal.hide();
                        location.reload();
                    } else {
                        alert('Error creating playlist: ' + (data.message || 'Unknown error'));
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('Error creating playlist');
                });
        });
    }

    // Handle save playlist modal
    const savePlaylistModal = document.getElementById('savePlaylistModal');
    if (savePlaylistModal) {
        savePlaylistModal.addEventListener('show.bs.modal', function(event) {
            const button = event.relatedTarget;
            const playlistId = button.getAttribute('data-playlist-id');
            const playlistName = button.getAttribute('data-playlist-name');

            currentSavePlaylistId = playlistId;
            document.getElementById('save-playlist-name').textContent = playlistName;
        });
    }

    // Handle delete playlist modal
    const deletePlaylistModal = document.getElementById('deletePlaylistModal');
    if (deletePlaylistModal) {
        deletePlaylistModal.addEventListener('show.bs.modal', function(event) {
            const button = event.relatedTarget;
            const playlistId = button.getAttribute('data-playlist-id');
            const playlistName = button.getAttribute('data-playlist-name');

            currentDeletePlaylistId = playlistId;
            document.getElementById('delete-playlist-name').textContent = playlistName;
        });
    }
});

function confirmSavePlaylist() {
    if (!currentSavePlaylistId) return;

    const confirmBtn = document.getElementById('confirmSavePlaylistBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<i class="bi bi-hourglass"></i> Saving...';

    fetch(`/api/playlists/${currentSavePlaylistId}/save`, {
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

function confirmDeletePlaylist() {
    if (!currentDeletePlaylistId) return;

    const confirmBtn = document.getElementById('confirmDeletePlaylistBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<i class="bi bi-hourglass"></i> Deleting...';

    fetch(`/api/playlists/${currentDeletePlaylistId}`, {
        method: 'DELETE'
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                const modal = bootstrap.Modal.getInstance(document.getElementById('deletePlaylistModal'));
                modal.hide();
                location.reload();
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