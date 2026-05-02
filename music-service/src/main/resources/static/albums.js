document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('search-input');
    const clearBtn = document.getElementById('clear-search');
    const resultsCount = document.getElementById('search-results-count');
    const matchingCountSpan = document.getElementById('matching-count');
    const totalCountSpan = document.getElementById('total-count');
    const albumCards = document.querySelectorAll('.col-md-3.col-sm-6.mb-4[data-album-title]');

    // Store original album data
    const albumsData = Array.from(albumCards).map(card => ({
        element: card,
        title: card.dataset.albumTitle || '',
        artist: card.dataset.artistName || '',
        songs: card.dataset.songTitles || '' // Thymeleaf renders List.toString(): "[title1, title2]"
    }));

    // Update total count
    totalCountSpan.textContent = albumsData.length;

    // Search function
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

        // Update results count display
        matchingCountSpan.textContent = matchCount;
        if (query) {
            resultsCount.style.display = 'block';
        } else {
            resultsCount.style.display = 'none';
        }
    }

    // Input event with debounce (150ms)
    let debounceTimer;
    searchInput.addEventListener('input', function() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            performSearch(this.value);
        }, 150);
    });

    // Clear button
    clearBtn.addEventListener('click', function() {
        searchInput.value = '';
        performSearch('');
        searchInput.focus();
    });
});