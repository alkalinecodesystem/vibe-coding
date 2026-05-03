document.addEventListener('DOMContentLoaded', function () {
    const sizeSelect = document.getElementById('sizeSelect');
    if (sizeSelect) {
        sizeSelect.addEventListener('change', function () {
            const size = this.value;
            const url = new URL(window.location);
            url.searchParams.set('size', size);
            url.searchParams.set('page', '0'); // Reset to first page
            // Keep other params
            window.location.href = url.toString();
        });
    }
});

function addSongToPlaylist(button) {
    const songId = button.getAttribute('data-song-id');

    // First, fetch available playlists (first page, 5 per page)
    const size = 5;
    fetch(`/api/playlists?page=0&size=${size}`)
        .then(response => response.json())
        .then(data => {
            if (data.success && data.data.content.length > 0) {
                showPlaylistSelectionModal(songId, data.data);
            } else if (data.success && data.data.totalElements > 0) {
                // If no playlists on first page but total > 0, show modal anyway (might have playlists on other pages)
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

function showPlaylistSelectionModal(songId, paginatedData) {
    const playlists = paginatedData.content;
    const currentPage = paginatedData.currentPage;
    const totalPages = paginatedData.totalPages;
    const hasPrevious = paginatedData.hasPrevious;
    const hasNext = paginatedData.hasNext;
    const isFirst = paginatedData.first;
    const isLast = paginatedData.last;



    // Ensure we always have 5 playlist slots (even if fewer playlists exist)
    const displayPlaylists = [...playlists];
    while (displayPlaylists.length < 5) {
        displayPlaylists.push(null); // Add empty slots
    }

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
                        <div class="list-group" id="playlist-list">
                            ${displayPlaylists.map(playlist => playlist ? `
                                <button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
                                        data-playlist-id="${playlist.id}"
                                        onclick="selectPlaylist(${playlist.id}, ${songId}, this)">
                                    <div>
                                        <div class="fw-bold">${playlist.name}</div>
                                        <small class="text-muted">${playlist.songCount} songs</small>
                                    </div>
                                    <i class="bi bi-plus-circle text-success"></i>
                                </button>
                            ` : `
                                <div class="list-group-item d-flex justify-content-between align-items-center opacity-25">
                                    <div>
                                        <div class="fw-bold text-muted">No playlist</div>
                                        <small class="text-muted">-</small>
                                    </div>
                                    <i class="bi bi-dash-circle text-muted"></i>
                                </div>
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

    // Add pagination dynamically
    addPaginationToModal(songId, paginatedData);

    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('selectPlaylistModal'));
    modal.show();
}

function loadPlaylistPage(songId, page, size = 5) {
    fetch(`/api/playlists?page=${page}&size=${size}`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                updatePlaylistModalContent(songId, data.data, size);
            } else {
                alert('Error loading playlists');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error loading playlists');
        });
}

function addPaginationToModal(songId, paginatedData, size = 5) {
    const currentPage = paginatedData.currentPage;
    const totalPages = paginatedData.totalPages;
    const hasPrevious = paginatedData.hasPrevious;
    const hasNext = paginatedData.hasNext;
    const isFirst = paginatedData.first;
    const isLast = paginatedData.last;

    const modalBody = document.querySelector('#selectPlaylistModal .modal-body');

    // Remove any existing pagination from modal body
    const existingPagination = modalBody.querySelector('#playlist-pagination');
    if (existingPagination) {
        existingPagination.remove();
    }

    if (totalPages > 1) {
        const paginationHtml = `
            <div id="playlist-pagination" class="d-flex justify-content-between align-items-center mt-3">
                <div class="d-flex align-items-center">
                    <label class="me-2">Show:</label>
                    <select id="playlistSizeSelect" class="form-select form-select-sm size-select">
                        <option value="5" ${size === 5 ? 'selected' : ''}>5</option>
                        <option value="10" ${size === 10 ? 'selected' : ''}>10</option>
                        <option value="15" ${size === 15 ? 'selected' : ''}>15</option>
                    </select>
                    <span class="ms-2 text-muted">per page</span>
                </div>
                <nav aria-label="Playlist pagination">
                    <ul class="pagination pagination-sm mb-0">
                        <li class="page-item ${isFirst ? 'disabled' : ''}">
                            <button class="page-link" onclick="loadPlaylistPage(${songId}, 0, ${size})" aria-label="First" ${isFirst ? 'disabled' : ''}>
                                <span aria-hidden="true">&laquo;&laquo;</span>
                            </button>
                        </li>
                        <li class="page-item ${!hasPrevious ? 'disabled' : ''}">
                            <button class="page-link" onclick="loadPlaylistPage(${songId}, ${currentPage - 1}, ${size})" aria-label="Previous" ${!hasPrevious ? 'disabled' : ''}>
                                <span aria-hidden="true">&laquo;</span>
                            </button>
                        </li>
 ${(() => {
                const startPage = Math.max(0, currentPage - 2);
                const endPage = Math.min(totalPages - 1, currentPage + 2);
                let pages = '';

                for (let i = startPage; i <= endPage; i++) {
                    if (i === currentPage) {
                        // Página actual (equivalente al <span>)
                        pages += `
                <li class="page-item active">
                    <span class="page-link">${i + 1}</span>
                </li>
            `;
                    } else {
                        // Otras páginas (equivalente al <a>)
                        pages += `
                <li class="page-item">
                    <button class="page-link"
                        onclick="loadPlaylistPage(${songId}, ${i}, ${size})">
                        ${i + 1}
                    </button>
                </li>
            `;
                    }
                }

                return pages;
            })()}                       <li class="page-item ${!hasNext ? 'disabled' : ''}">
                            <button class="page-link" onclick="loadPlaylistPage(${songId}, ${currentPage + 1}, ${size})" aria-label="Next" ${!hasNext ? 'disabled' : ''}>
                                <span aria-hidden="true">&raquo;</span>
                            </button>
                        </li>
                        <li class="page-item ${isLast ? 'disabled' : ''}">
                            <button class="page-link" onclick="loadPlaylistPage(${songId}, ${totalPages - 1}, ${size})" aria-label="Last" ${isLast ? 'disabled' : ''}>
                                <span aria-hidden="true">&raquo;&raquo;</span>
                            </button>
                        </li>
                    </ul>
                </nav>
            </div>
        `;

        // Insert pagination at the end of modal body (after playlist list)
        modalBody.insertAdjacentHTML('beforeend', paginationHtml);

        // Add event listener for size selector (remove existing ones first)
        const sizeSelect = document.getElementById('playlistSizeSelect');
        if (sizeSelect) {
            // Remove existing event listeners by cloning and replacing the element
            const newSizeSelect = sizeSelect.cloneNode(true);
            sizeSelect.parentNode.replaceChild(newSizeSelect, sizeSelect);

            newSizeSelect.addEventListener('change', function () {
                const newSize = parseInt(this.value);
                loadPlaylistPage(songId, 0, newSize);
            });
        }
    }
}

function updatePlaylistModalContent(songId, paginatedData, size = 5) {
    const playlists = paginatedData.content;
    const currentPage = paginatedData.currentPage;
    const totalPages = paginatedData.totalPages;
    const hasPrevious = paginatedData.hasPrevious;
    const hasNext = paginatedData.hasNext;
    const isFirst = paginatedData.first;
    const isLast = paginatedData.last;

    // Ensure we always have the specified number of playlist slots
    const displayPlaylists = [...playlists];
    while (displayPlaylists.length < 5) {
        displayPlaylists.push(null); // Add empty slots
    }

    // Update playlist list
    const playlistList = document.getElementById('playlist-list');
    playlistList.innerHTML = displayPlaylists.map(playlist => playlist ? `
        <button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
                data-playlist-id="${playlist.id}"
                onclick="selectPlaylist(${playlist.id}, ${songId}, this)">
            <div>
                <div class="fw-bold">${playlist.name}</div>
                <small class="text-muted">${playlist.songCount} songs</small>
            </div>
            <i class="bi bi-plus-circle text-success"></i>
        </button>
    ` : `
        <div class="list-group-item d-flex justify-content-between align-items-center opacity-25">
            <div>
                <div class="fw-bold text-muted">No playlist</div>
                <small class="text-muted">-</small>
            </div>
            <i class="bi bi-dash-circle text-muted"></i>
        </div>
    `).join('');

    // Update pagination
    addPaginationToModal(songId, paginatedData, size);
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