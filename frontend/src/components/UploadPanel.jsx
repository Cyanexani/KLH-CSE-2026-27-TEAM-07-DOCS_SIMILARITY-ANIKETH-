import { useRef, useState } from 'react'

const SUPPORTED_EXTENSIONS = ['txt', 'pdf', 'docx']

function extensionOf(filename) {
  const dot = filename.lastIndexOf('.')
  if (dot < 0) return ''
  return filename.slice(dot + 1).toLowerCase()
}

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

export default function UploadPanel({ files, onAddFiles, onRemoveFile, onAnalyze, analyzing, error }) {
  const inputRef = useRef(null)
  const [dragging, setDragging] = useState(false)

  const handleFiles = (fileList) => {
    const incoming = Array.from(fileList)
    onAddFiles(incoming)
  }

  return (
    <div className="upload-panel">
      <p className="section-label">Documents</p>

      <div
        className={`dropzone${dragging ? ' dragging' : ''}`}
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault()
          setDragging(false)
          handleFiles(e.dataTransfer.files)
        }}
      >
        <div className="dropzone-label">
          <strong>Add Documents</strong> — click or drag files here
        </div>
        <div className="dropzone-sub">Supported formats: TXT, PDF, DOCX — max 25 MB per file</div>
        <input
          ref={inputRef}
          type="file"
          accept=".txt,.pdf,.docx"
          multiple
          hidden
          onChange={(e) => {
            handleFiles(e.target.files)
            e.target.value = ''
          }}
        />
      </div>

      {files.length > 0 && (
        <ul className="file-list">
          {files.map((f, i) => {
            const ext = extensionOf(f.name)
            const supported = SUPPORTED_EXTENSIONS.includes(ext)
            return (
              <li key={f.name + i} className={supported ? '' : 'file-item-unsupported'}>
                <span>
                  <span className={`format-badge${supported ? '' : ' format-badge-error'}`}>
                    {ext ? ext.toUpperCase() : '?'}
                  </span>
                  {f.name}{' '}
                  <span className="hint-text">({formatBytes(f.size)})</span>
                  {!supported && <span className="hint-text-error"> — unsupported format</span>}
                </span>
                <button className="file-remove" onClick={() => onRemoveFile(i)}>remove</button>
              </li>
            )
          })}
        </ul>
      )}

      <div className="upload-actions">
        <button
          className="btn-primary"
          disabled={files.length < 2 || analyzing}
          onClick={onAnalyze}
        >
          {analyzing ? 'ANALYZING…' : 'ANALYZE DOCUMENTS'}
        </button>
        <span className="hint-text">
          {files.length < 2
            ? 'Select at least 2 documents (TXT, PDF, or DOCX) to begin'
            : `${files.length} documents selected`}
        </span>
      </div>

      {error && <div className="error-banner">{error}</div>}
    </div>
  )
}
