import { useState } from 'react'

const PAGE_SIZE = 20

export default function AlgorithmDetails({ details }) {
  const [page, setPage] = useState(0)
  const sample = details.suffixArraySample || []
  const totalPages = Math.max(1, Math.ceil(sample.length / PAGE_SIZE))
  const pageRows = sample.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)

  const statusRows = [
    ['Documents analyzed', details.documentsAnalyzed],
    ['Combined token count', `${details.combinedTokenCount.toLocaleString()} tokens`],
    ['Suffix Array status', details.suffixArrayBuilt ? <span className="status-badge">✓ built</span> : 'not built'],
    ['LCP Array status', details.lcpArrayBuilt ? <span className="status-badge">✓ built</span> : 'not built'],
    ['Kasai Algorithm status', details.kasaiAlgorithmUsed ? <span className="status-badge">✓ used</span> : 'not used'],
    ['Common word sequences found', details.totalCommonSequencesFound],
    ['Document pairs compared', details.documentPairsCompared],
    ['Min sequence length', `${details.minSequenceWords} words`],
    ['Suffix Array total entries', details.suffixArrayTotalEntries.toLocaleString()],
  ]

  return (
    <div className="details-panel">
      <p className="section-label">Algorithm Details</p>

      <div className="status-grid">
        {statusRows.map(([label, value]) => (
          <div className="status-row" key={label}>
            <span>{label}</span>
            <span>{value}</span>
          </div>
        ))}
      </div>

      <p className="section-label">Suffix Array / LCP — technical view</p>
      <p className="hint-text" style={{ marginBottom: 10 }}>
        Showing {sample.length.toLocaleString()} of {details.suffixArrayTotalEntries.toLocaleString()} entries
        (limited sample; the full array is used internally for analysis).
      </p>

      <div className="sa-table-wrap">
        <table className="sa-table">
          <thead>
            <tr>
              <th>SA Index</th>
              <th>Suffix Position</th>
              <th>Document</th>
              <th>Word Offset</th>
              <th>LCP</th>
              <th>Suffix preview</th>
            </tr>
          </thead>
          <tbody>
            {pageRows.map((row) => (
              <tr key={row.saIndex}>
                <td>{row.saIndex}</td>
                <td>{row.suffixPosition}</td>
                <td>{row.documentName}</td>
                <td>{row.offsetInDocument}</td>
                <td>{row.lcp}</td>
                <td className="sa-preview">{row.suffixPreview}…</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="pagination-controls">
        <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>← prev</button>
        <span>page {page + 1} of {totalPages}</span>
        <button disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>next →</button>
      </div>
    </div>
  )
}
