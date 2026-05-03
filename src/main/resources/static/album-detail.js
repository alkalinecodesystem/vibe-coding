async function deleteAlbum(albumId) {
    // Show confirm dialog
    if (!confirm('Are you sure you want to delete this album? This will also delete all associated songs.')) {
        return;
    }

    try {
        const response = await fetch('/albums/' + albumId + '/delete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        });

        if (response.redirected) {
            // If redirected (successful delete), go to the redirected URL
            window.location.href = response.url;
        } else {
            alert('Album and associated songs deleted successfully!');
            window.location.href = '/albums';
        }
    } catch (error) {
        console.error('Delete error:', error);
        alert('Error deleting album: ' + error.message);
    }
}