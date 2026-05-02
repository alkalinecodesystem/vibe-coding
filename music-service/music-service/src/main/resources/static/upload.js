let selectedFile = null;

// Dropzone setup
const dropZone = document.getElementById('dropZone');
const fileInput = document.getElementById('fileInput');
const dropContent = document.getElementById('dropContent');
const fileInfo = document.getElementById('fileInfo');
const fileName = document.getElementById('fileName');
const fileSize = document.getElementById('fileSize');
const uploadBtn = document.getElementById('uploadBtn');

// Click to select file
dropZone.addEventListener('click', () => fileInput.click());

// Drag and drop handlers
dropZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropZone.classList.add('dragover');
});

dropZone.addEventListener('dragleave', () => {
    dropZone.classList.remove('dragover');
});

dropZone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropZone.classList.remove('dragover');
    if (e.dataTransfer.files.length) {
        handleFile(e.dataTransfer.files[0]);
    }
});

// File input change
fileInput.addEventListener('change', (e) => {
    if (e.target.files.length) {
        handleFile(e.target.files[0]);
    }
});

function handleFile(file) {
    if (!file.name.toLowerCase().endsWith('.zip')) {
        alert('Please select a ZIP file');
        return;
    }
    if (file.size > 500 * 1024 * 1024) {
        alert('File size exceeds 500MB limit');
        return;
    }

    selectedFile = file;
    fileName.textContent = file.name;
    fileSize.textContent = formatBytes(file.size);
    dropContent.style.display = 'none';
    fileInfo.style.display = 'block';
    uploadBtn.disabled = false;
}

function formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function handleUpload(event) {
    event.preventDefault();
    if (!selectedFile) return;

    const formData = new FormData();
    formData.append('file', selectedFile);

    uploadBtn.disabled = true;
    document.getElementById('progressContainer').style.display = 'block';
    document.getElementById('progressBar').style.width = '0%';
    document.getElementById('progressBar').textContent = '0%';

    const xhr = new XMLHttpRequest();

    xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
            const percent = Math.round((e.loaded / e.total) * 100);
            document.getElementById('progressBar').style.width = percent + '%';
            document.getElementById('progressBar').textContent = percent + '%';
            document.getElementById('progressText').textContent =
                `Uploaded: ${formatBytes(e.loaded)} of ${formatBytes(e.total)}`;
        }
    });

    xhr.addEventListener('load', () => {
        uploadBtn.disabled = false;
        try {
            const response = JSON.parse(xhr.responseText);
            const alertDiv = document.getElementById('resultAlert');
            const title = document.getElementById('resultTitle');
            const message = document.getElementById('resultMessage');

            if (response.success) {
                alertDiv.className = 'alert alert-success';
                title.textContent = 'Success!';
                // Refresh page after a delay to show new data
                setTimeout(() => location.reload(), 2000);
            } else {
                alertDiv.className = 'alert alert-danger';
                title.textContent = 'Error';
            }
            message.textContent = response.message || JSON.stringify(response, null, 2);
            document.getElementById('resultContainer').style.display = 'block';
        } catch (e) {
            alert('Error processing response: ' + e.message);
        }
    });

    xhr.addEventListener('error', () => {
        uploadBtn.disabled = false;
        alert('Upload failed. Please try again.');
    });

    xhr.open('POST', '/api/upload/zip');
    xhr.send(formData);
}