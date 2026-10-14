import { useState } from 'react'
import UploadPanel from './components/UploadPanel.jsx'
import ProgressChecklist from './components/ProgressChecklist.jsx'
import SummaryCards from './components/SummaryCards.jsx'
import DocumentsList from './components/DocumentsList.jsx'
import RankingTable from './components/RankingTable.jsx'
import PairDetail from './components/PairDetail.jsx'
import Pipeline from './components/Pipeline.jsx'
import AlgorithmDetails from './components/AlgorithmDetails.jsx'

const API_BASE = 'http://localhost:8080/api'
const SUPPORTED_EXTENSIONS = ['txt', 'pdf', 'docx']

// Mirrors the backend limits (application.properties / CorsConfig.multipartConfigElement):
// max 25MB per individual file, max 100MB for the whole multipart request.
const MAX_FILE_SIZE_MB = 25
const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024
const MAX_TOTAL_SIZE_MB = 100
const MAX_TOTAL_SIZE_BYTES = MAX_TOTAL_SIZE_MB * 1024 * 1024

function extensionOf(filename) {
  const dot = filename.lastIndexOf('.')
  if (dot < 0) return ''
  return filename.slice(dot + 1).toLowerCase()
}

function formatMB(bytes) {
  return (bytes / (1024 * 1024)).toFixed(2)
}

// Last index of the progress list App drives automatically while the
// request is in flight; the final stage ("Documents ranked") is only
// shown once the server actually responds successfully.
const LAST_AUTO_PROGRESS_INDEX = 6
const FINAL_PROGRESS_INDEX = 7

export default function App() {
  const [files, setFiles] = useState([])
  const [analyzing, setAnalyzing] = useState(false)
  const [progressIndex, setProgressIndex] = useState(-1)
  const [error, setError] = useState(null)
  const [result, setResult] = useState(null)
  const [selectedPairKey, setSelectedPairKey] = useState(null)

  const handleAddFiles = (incoming) => {
    setError(null)
    setFiles((prev) => {
      const existingNames = new Set(prev.map((f) => f.name))
      const merged = [...prev]
      let rejectionMessage = null

      for (const f of incoming) {
        const ext = extensionOf(f.name)

        if (!SUPPORTED_EXTENSIONS.includes(ext)) {
          rejectionMessage = `Unsupported or invalid file: '${f.name}'. Supported formats are TXT, PDF, and DOCX.`
          continue
        }
        if (f.size === 0) {
          rejectionMessage = `Unsupported or invalid file: '${f.name} (empty file)'. Supported formats are TXT, PDF, and DOCX.`
          continue
        }
        if (f.size > MAX_FILE_SIZE_BYTES) {
          rejectionMessage = `'${f.name}' is ${formatMB(f.size)} MB, which exceeds the ${MAX_FILE_SIZE_MB} MB limit per file. Please upload a smaller file.`
          continue
        }
        if (!existingNames.has(f.name)) {
          merged.push(f)
          existingNames.add(f.name)
        }
      }

      const totalSize = merged.reduce((sum, f) => sum + f.size, 0)
      if (totalSize > MAX_TOTAL_SIZE_BYTES) {
        rejectionMessage = `The combined size of selected documents (${formatMB(totalSize)} MB) exceeds the ${MAX_TOTAL_SIZE_MB} MB total upload limit. Please remove some files.`
      }

      if (rejectionMessage) {
        setError(rejectionMessage)
      }
      return merged
    })
  }

  const handleRemoveFile = (index) => {
    setFiles((prev) => prev.filter((_, i) => i !== index))
  }

  const handleAnalyze = async () => {
    setError(null)
    setResult(null)
    setSelectedPairKey(null)

    if (files.length < 2) {
      setError('Please select at least 2 documents before analyzing.')
      return
    }

    const badFile = files.find((f) => !SUPPORTED_EXTENSIONS.includes(extensionOf(f.name)))
    if (badFile) {
      setError(`Unsupported file type: '${badFile.name}'. Supported formats are TXT, PDF, and DOCX.`)
      return
    }

    const oversizedFile = files.find((f) => f.size > MAX_FILE_SIZE_BYTES)
    if (oversizedFile) {
      setError(`'${oversizedFile.name}' is ${formatMB(oversizedFile.size)} MB, which exceeds the ${MAX_FILE_SIZE_MB} MB limit per file. Please upload a smaller file.`)
      return
    }

    const totalSize = files.reduce((sum, f) => sum + f.size, 0)
    if (totalSize > MAX_TOTAL_SIZE_BYTES) {
      setError(`The combined size of selected documents (${formatMB(totalSize)} MB) exceeds the ${MAX_TOTAL_SIZE_MB} MB total upload limit. Please remove some files.`)
      return
    }

    setAnalyzing(true)
    setProgressIndex(0)

    // Drive the visual progress checklist forward while the request is in flight.
    // The real work (including format-specific text extraction) happens
    // server-side; this only paces the UI so the stages are legible rather
    // than flashing by instantly.
    const progressTimer = setInterval(() => {
      setProgressIndex((prev) => (prev < LAST_AUTO_PROGRESS_INDEX ? prev + 1 : prev))
    }, 220)

    try {
      const formData = new FormData()
      files.forEach((f) => formData.append('files', f))

      const response = await fetch(`${API_BASE}/analyze`, {
        method: 'POST',
        body: formData,
      })

      if (!response.ok) {
        // The server enforces the same 25MB/100MB limits; if a file somehow gets past
        // the client-side checks above, a 413 (Payload Too Large) may come back as a
        // raw server error page rather than our JSON error shape, so we can't always
        // assume response.json() will succeed here.
        let message = `Analysis failed with status ${response.status}`
        if (response.status === 413) {
          message = `The upload was rejected because it exceeds the server's size limit (max ${MAX_FILE_SIZE_MB} MB per file, ${MAX_TOTAL_SIZE_MB} MB total). Please use smaller files.`
        } else {
          try {
            const data = await response.json()
            if (data?.error) message = data.error
          } catch {
            // Response wasn't JSON (e.g. a raw server error page) — keep the fallback message above.
          }
        }
        throw new Error(message)
      }

      const data = await response.json()

      setProgressIndex(FINAL_PROGRESS_INDEX)
      setTimeout(() => {
        setResult(data)
        setAnalyzing(false)
      }, 250)
    } catch (err) {
      clearInterval(progressTimer)
      setAnalyzing(false)
      setProgressIndex(-1)
      if (err instanceof TypeError) {
        setError('Could not reach the backend at http://localhost:8080. Make sure the Spring Boot server is running.')
      } else {
        setError(err.message || 'An unexpected error occurred during analysis.')
      }
      return
    }
    clearInterval(progressTimer)
  }

  const selectedPair = result?.ranking.find(
    (r) => `${r.docAId}-${r.docBId}` === selectedPairKey
  ) || null

  return (
    <div className="app-shell">
      <header className="app-header">
        <h1 className="app-title">DOCS_SIMILARITY</h1>
        <p className="app-subtitle">Document Similarity Analyzer</p>
      </header>

      <UploadPanel
        files={files}
        onAddFiles={handleAddFiles}
        onRemoveFile={handleRemoveFile}
        onAnalyze={handleAnalyze}
        analyzing={analyzing}
        error={error}
      />

      {analyzing && (
        <>
          <hr className="section-divider" />
          <ProgressChecklist activeIndex={progressIndex} />
        </>
      )}

      {result && !analyzing && (
        <div className="results-section">
          <hr className="section-divider" />
          <SummaryCards summary={result.summary} />

          <hr className="section-divider" />
          <DocumentsList documents={result.documents} />

          <hr className="section-divider" />
          <RankingTable
            ranking={result.ranking}
            selectedPairKey={selectedPairKey}
            onSelectPair={setSelectedPairKey}
          />

          <hr className="section-divider" />
          <PairDetail pair={selectedPair} />

          <hr className="section-divider" />
          <Pipeline />

          <hr className="section-divider" />
          <AlgorithmDetails details={result.algorithmDetails} />
        </div>
      )}

      <p className="footer-note">DOCS_SIMILARITY — DSA-3 Project · Suffix Array + Kasai LCP Similarity Engine · Supports TXT, PDF, DOCX</p>
    </div>
  )
}
