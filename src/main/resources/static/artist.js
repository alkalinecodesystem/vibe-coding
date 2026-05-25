let currentArtistTriggerButton = null;

function initArtist() {
	const viewArtistButtons = document.querySelectorAll('.view-artist-btn');
	viewArtistButtons.forEach(button => {
		if (button.dataset.artistListenerAttached === 'true') return;
		button.dataset.artistListenerAttached = 'true';
		button.addEventListener('click', async function (event) {
			this.blur(); // immediately remove focus so it doesn't stay colored
			currentArtistTriggerButton = this;

			const artistId = this.getAttribute('data-artist-id');
			const artistName = this.getAttribute('data-artist-name');

			if (!artistId) {
				alert('Error: No artist ID found.');
				// Close the modal
				const modal = bootstrap.Modal.getInstance(document.getElementById('artistModal'));
				if (modal) modal.hide();
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

				if (!artist) {
					alert('No artist data received from the server.');
					const modal = bootstrap.Modal.getInstance(document.getElementById('artistModal'));
					if (modal) modal.hide();
					return;
				}

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
                               ${artist.albums && artist.albums.length > 0 ? (() => {
					const randomAlbums = artist.albums.sort(() => 0.5 - Math.random()).slice(0, 5);
					return randomAlbums.map(album => `<li class="list-group-item">
                                     <strong>${album.title}</strong> ${album.releaseYear ? `(${album.releaseYear})` : ''}
                                   </li>`).join('');
				})() : '<li class="list-group-item text-muted">No albums found</li>'}
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

				biographyTextarea.addEventListener('input', function () {
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
				document.getElementById('save-artist-btn').addEventListener('click', async function () {
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
				modalElement.addEventListener('hidden.bs.modal', function () {
					const modalInstance = bootstrap.Modal.getInstance(modalElement);
					if (modalInstance) {
						modalInstance.dispose();
					}
				});

			} catch (err) {
				alert('Could not load artist details: ' + err.message);
				// Close the modal if fetch failed
				const modal = bootstrap.Modal.getInstance(document.getElementById('artistModal'));
				if (modal) modal.hide();
			}
		});
	});

	// Extra safety: blur any View Artist buttons on click + after the artist modal closes
	document.querySelectorAll('.view-artist-btn').forEach(btn => {
		if (btn.dataset.blurListenerAttached === 'true') return;
		btn.dataset.blurListenerAttached = 'true';
		btn.addEventListener('click', () => {
			requestAnimationFrame(() => btn.blur());
		});
	});

	const artistModalEl2 = document.getElementById('artistModal');
	if (artistModalEl2) {
		artistModalEl2.addEventListener('hidden.bs.modal', () => {
			const el = document.activeElement;
			if (el && el.classList.contains('btn-outline-primary') && !el.closest('.modal')) {
				el.blur();
			}
		});
	}
}

initArtist();
