const STAGES = [
  'Documents',
  'Tokenization',
  'Token Sequence',
  'Suffix Array',
  'Kasai Algorithm',
  'LCP Array',
  'Common Word Sequences',
  'Similarity Scores',
  'Document Ranking',
]

export default function Pipeline() {
  return (
    <div className="pipeline-panel">
      <p className="section-label">Algorithm Pipeline</p>
      <div className="pipeline-flow">
        {STAGES.map((stage, i) => (
          <span key={stage} style={{ display: 'flex', alignItems: 'center' }}>
            <span className="pipeline-node">{stage}</span>
            {i < STAGES.length - 1 && <span className="pipeline-arrow">→</span>}
          </span>
        ))}
      </div>
    </div>
  )
}
