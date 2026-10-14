export default function DocumentsList({ documents }) {
  return (
    <div>
      <p className="section-label">Documents &amp; Extraction Status</p>
      <table className="ranking-table">
        <thead>
          <tr>
            <th>Filename</th>
            <th>Format</th>
            <th>Extracted Length</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {documents.map((doc) => (
            <tr key={doc.id}>
              <td>{doc.filename}</td>
              <td><span className="format-badge">{doc.sourceFormat}</span></td>
              <td>{doc.wordCount.toLocaleString()} words</td>
              <td><span className="status-badge">✓ {doc.extractionStatus.toLowerCase()}</span></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
