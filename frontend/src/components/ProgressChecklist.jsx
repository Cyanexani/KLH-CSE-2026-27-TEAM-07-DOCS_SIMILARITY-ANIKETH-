const ALL_STAGES = [
  'Documents loaded',
  'Text extracted from all documents (TXT/PDF/DOCX)',
  'Text normalized and tokenized into word IDs',
  'Suffix Array constructed',
  'LCP Array constructed',
  'Common word sequences identified',
  'Similarity scores calculated',
  'Documents ranked',
]

export default function ProgressChecklist({ activeIndex }) {
  return (
    <div className="progress-panel">
      <p className="section-label">Analyzing Documents…</p>
      <ul className="progress-list">
        {ALL_STAGES.map((stage, i) => {
          const done = i <= activeIndex
          return (
            <li key={stage} className={`progress-item ${done ? 'done' : 'pending'}`}>
              <span className="progress-mark">{done ? '✓' : '○'}</span>
              {stage}
            </li>
          )
        })}
      </ul>
    </div>
  )
}
