export default function SummaryCards({ summary }) {
  const cards = [
    { label: 'Documents', value: summary.documentsAnalyzed },
    { label: 'Pairs Compared', value: summary.pairsCompared },
    { label: 'Common Word Sequences', value: summary.commonSegmentsFound },
    { label: 'Highest Similarity', value: `${summary.highestSimilarity.toFixed(2)}%` },
  ]

  return (
    <div>
      <p className="section-label">Analysis Summary</p>
      <div className="summary-grid">
        {cards.map((c) => (
          <div className="summary-card" key={c.label}>
            <div className="summary-value">{c.value}</div>
            <div className="summary-label">{c.label}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
