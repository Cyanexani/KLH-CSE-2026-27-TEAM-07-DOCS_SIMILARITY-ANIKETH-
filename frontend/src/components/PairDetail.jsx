export default function PairDetail({ pair }) {
  if (!pair) {
    return (
      <div className="pair-detail">
        <p className="section-label">Selected Comparison</p>
        <div className="empty-note">Click a row in the ranking table above to inspect the common word sequences for that pair.</div>
      </div>
    )
  }

  const segments = pair.topCommonSequences || []

  return (
    <div className="pair-detail">
      <div className="pair-detail-header">
        <span className="pair-detail-title">{pair.docAName} ↔ {pair.docBName}</span>
        <span className="pair-detail-sim">{pair.similarityPercent.toFixed(2)}%</span>
      </div>
      <div className="pair-detail-sub">
        {pair.totalCommonSequences} common word sequence{pair.totalCommonSequences === 1 ? '' : 's'} found
        {segments.length < pair.totalCommonSequences && ` — showing the ${segments.length} longest`}
      </div>

      {segments.length === 0 ? (
        <div className="empty-note">No qualifying common word sequences were found between these documents.</div>
      ) : (
        <div className="segment-list">
          {segments.map((s, i) => (
            <div className="segment-item" key={i}>
              <div className="segment-text">&ldquo;{s.text}&rdquo;</div>
              <div className="segment-meta">
                Length: {s.wordCount} words · word offset in {pair.docAName}: {s.offsetInA} · word offset in {pair.docBName}: {s.offsetInB}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
