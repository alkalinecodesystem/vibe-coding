var currentArtistModalId = null;
var currentArtistName = null;
var currentArtistModalSize = 5;

document.addEventListener('DOMContentLoaded', function () {
  if (window._artistAlbumsWired !== true) {
    window._artistAlbumsWired = true;
    bindExistingAlbumSizeSelect();
    bindExistingViewAlbumButtons();
  }
  if (document.getElementById('artistAlbumsModal')) {
    wireSizeSelect();
  }
});

function bindExistingAlbumSizeSelect() {
  var sizeSelect = document.getElementById('artistAlbumSizeSelect');
  if (!sizeSelect || sizeSelect.dataset.wired === 'true') return;
  sizeSelect.dataset.wired = 'true';
  sizeSelect.addEventListener('change', function () {
    var artistId = document.getElementById('artist-albums-modal-table-body')?.dataset?.artistId;
    var artistName = document.getElementById('artistAlbumsModalLabel')?.textContent?.replace('Albums - ', '');
    if (!artistId) return;
    currentArtistModalId = artistId;
    currentArtistName = artistName;
    currentArtistModalSize = parseInt(this.value, 10) || 5;
    loadArtistAlbumsModalPage(artistId, artistName, 0, currentArtistModalSize);
  });
}

function bindExistingViewAlbumButtons() {
  document.querySelectorAll('.view-artist-albums-btn').forEach(function (btn) {
    if (btn.dataset.wired === 'true') return;
    btn.dataset.wired = 'true';
    btn.addEventListener('click', function () {
      this.blur();
      var artistId = this.getAttribute('data-artist-id');
      var artistName = this.getAttribute('data-artist-name');
      openArtistAlbumsModal(artistId, artistName);
    });
  });
}

function openArtistAlbumsModal(artistId, artistName) {
  if (!artistName) return;
  currentArtistModalId = artistId;
  currentArtistName = artistName;
  currentArtistModalSize = 5;
  ensureArtistAlbumsModal();
  var titleEl = document.getElementById('artistAlbumsModalLabel');
  if (titleEl) titleEl.textContent = 'Albums - ' + artistName;
  var sizeSelect = document.getElementById('artistAlbumSizeSelect');
  if (sizeSelect) sizeSelect.value = '5';
  loadArtistAlbumsModalPage(artistId, artistName, 0, 5);
}

function ensureArtistAlbumsModal() {
  if (document.getElementById('artistAlbumsModal')) return;
  var html =
    '<div class="modal fade" id="artistAlbumsModal" tabindex="-1" aria-labelledby="artistAlbumsModalLabel" aria-hidden="true">' +
      '<div class="modal-dialog modal-lg">' +
        '<div class="modal-content">' +
          '<div class="modal-header">' +
            '<h5 class="modal-title" id="artistAlbumsModalLabel">Artist Albums</h5>' +
            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
          '</div>' +
          '<div class="modal-body">' +
            '<div class="modal-albums-scroll">' +
              '<table class="table table-hover align-middle">' +
                '<thead class="table-light">' +
                  '<tr>' +
                    '<th>Title</th>' +
                    '<th>Year</th>' +
                    '<th>Genre</th>' +
                    '<th>Songs</th>' +
                  '</tr>' +
                '</thead>' +
                '<tbody id="artist-albums-modal-table-body"></tbody>' +
              '</table>' +
            '</div>' +
            '<div id="artist-albums-modal-pagination-wrapper" class="me-auto"></div>' +
            '<div class="d-flex align-items-center">' +
              '<label class="me-2">Show:</label>' +
              '<select id="artistAlbumSizeSelect" class="form-select form-select-sm size-select">' +
                '<option value="5" selected>5</option>' +
                '<option value="10">10</option>' +
                '<option value="15">15</option>' +
              '</select>' +
              '<span class="ms-2 text-muted">per page</span>' +
            '</div>' +
          '</div>' +
          '<div class="modal-footer">' +
            '<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>' +
          '</div>' +
        '</div>' +
      '</div>' +
    '</div>';
  document.body.insertAdjacentHTML('beforeend', html);
  var modalEl = document.getElementById('artistAlbumsModal');
  if (modalEl) {
    new bootstrap.Modal(modalEl, { backdrop: true, keyboard: true, focus: true });
    modalEl.addEventListener('hidden.bs.modal', function () {
      if (bootstrap.Modal.getInstance(modalEl)) bootstrap.Modal.getInstance(modalEl).dispose();
      modalEl.remove();
    });
  }
  wireSizeSelect();
}

function wireSizeSelect() {
  var sizeSelect = document.getElementById('artistAlbumSizeSelect');
  if (!sizeSelect || sizeSelect.dataset.wired === 'true') return;
  sizeSelect.dataset.wired = 'true';
  sizeSelect.addEventListener('change', function () {
    currentArtistModalSize = parseInt(this.value, 10) || 5;
    if (currentArtistModalId) {
      loadArtistAlbumsModalPage(currentArtistModalId, currentArtistName, 0, currentArtistModalSize);
    }
  });
}

function loadArtistAlbumsModalPage(artistId, artistName, page, size) {
  var baseUrl = '/api/albums/by-artist/' + encodeURIComponent(artistId || '');
  var url = baseUrl + '?page=' + page + '&size=' + size + '&artistName=' + encodeURIComponent(artistName || '');
  fetch(url)
    .then(function (r) { return r.json(); })
    .then(function (json) {
      if (!json.success || !json.data) { alert('Error loading albums'); return; }
      refreshArtistAlbumsModal(json.data, artistId, artistName, size);
    })
    .catch(function (err) { alert('Error loading albums: ' + err.message); });
}

function refreshArtistAlbumsModal(paginated, artistId, artistName, size) {
  ensureArtistAlbumsModal();
  var tableBody = document.getElementById('artist-albums-modal-table-body');
  var paginationContainer = document.getElementById('artist-albums-modal-pagination-wrapper');
  if (!tableBody || !paginationContainer) return;
  tableBody.dataset.artistId = artistId;
  tableBody.innerHTML = '';
  paginationContainer.innerHTML = '';
  var sizeSelect = document.getElementById('artistAlbumSizeSelect');
  if (sizeSelect) sizeSelect.value = String(size || 5);

  if (!paginated || !paginated.content || paginated.content.length === 0) {
    tableBody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No albums for this artist.</td></tr>';
    showArtistModal();
    return;
  }

  paginated.content.forEach(function (album, idx) {
    var year = album.releaseYear ? album.releaseYear : '-';
    var genre = album.genere ? '<span class="badge bg-secondary">' + escapeHtml(album.genere) + '</span>' : '-';
    var count = album.songs ? album.songs.length : 0;
    var tr = document.createElement('tr');
    tr.innerHTML =
        '<td><strong>' + escapeHtml(album.title) + '</strong></td>' +
        '<td>' + year + '</td>' +
        '<td>' + genre + '</td>' +
        '<td>' + count + '</td>';
    tableBody.appendChild(tr);
  });

  if ((paginated.totalPages || 0) > 1 && paginated) {
    paginationContainer.innerHTML = buildModalPaginationInner(paginated, artistId, artistName, size);
  }

  showArtistModal();
}

function showArtistModal() {
  var modalEl = document.getElementById('artistAlbumsModal');
  if (!modalEl) return;
  var modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
  modal.show();
}

window._artistAlbumsGoTo = function (artistId, artistName, page, size) {
  if (artistId || artistName) loadArtistAlbumsModalPage(artistId, artistName, page, size || currentArtistModalSize || 5);
};

function buildModalPaginationInner(paginated, artistId, artistName, size) {
  size = size || currentArtistModalSize || 5;
  var currentPage = paginated.currentPage;
  var totalPages = paginated.totalPages;
  var hasPrevious = paginated.hasPrevious;
  var hasNext = paginated.hasNext;
  var isLast = paginated.last;

  var startPage = Math.max(0, currentPage - 2);
  var endPage = Math.min(totalPages - 1, currentPage + 2);
  var pages = '';
  for (var i = startPage; i <= endPage; i++) {
    pages +=
      '<li class="page-item ' + (i === currentPage ? 'active' : '') + '">' +
        '<button class="page-link" type="button" onclick="window._artistAlbumsGoTo(' + artistId + ', \'' + escapeJs(artistName) + '\', ' + i + ', ' + size + ')" ' + (i === currentPage ? 'disabled' : '') + '>' + (i + 1) + '</button>' +
      '</li>';
  }

  return '' +
    '<nav aria-label="Artist albums pagination"><ul class="pagination pagination-sm mb-0">' +
      '<li class="page-item ' + (!hasPrevious ? 'disabled' : '') + '"><button class="page-link" type="button" onclick="window._artistAlbumsGoTo(' + artistId + ', \'' + escapeJs(artistName) + '\', 0, ' + size + ')" ' + (!hasPrevious ? 'disabled' : '') + ' aria-label="First"><span aria-hidden="true">&laquo;&laquo;</span></button></li>' +
      '<li class="page-item ' + (!hasPrevious ? 'disabled' : '') + '"><button class="page-link" type="button" onclick="window._artistAlbumsGoTo(' + artistId + ', \'' + escapeJs(artistName) + '\', ' + (currentPage - 1) + ', ' + size + ')" ' + (!hasPrevious ? 'disabled' : '') + ' aria-label="Previous"><span aria-hidden="true">&laquo;</span></button></li>' +
      pages +
      '<li class="page-item ' + (!hasNext ? 'disabled' : '') + '"><button class="page-link" type="button" onclick="window._artistAlbumsGoTo(' + artistId + ', \'' + escapeJs(artistName) + '\', ' + (currentPage + 1) + ', ' + size + ')" ' + (!hasNext ? 'disabled' : '') + ' aria-label="Next"><span aria-hidden="true">&raquo;</span></button></li>' +
      '<li class="page-item ' + (isLast ? 'disabled' : '') + '"><button class="page-link" type="button" onclick="window._artistAlbumsGoTo(' + artistId + ', \'' + escapeJs(artistName) + '\', ' + (totalPages - 1) + ', ' + size + ')" ' + (isLast ? 'disabled' : '') + ' aria-label="Last"><span aria-hidden="true">&raquo;&raquo;</span></button></li>' +
    '</ul></nav>';
}

function escapeJs(text) {
  if (text == null) return '';
  return String(text).replace(/\\/g, '\\\\').replace(/'/g, '\\\'').replace(/"/g, '&quot;');
}

function escapeHtml(text) {
  var div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}
