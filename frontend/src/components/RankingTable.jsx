export default function RankingTable({ ranking, selectedPairKey, onSelectPair }) {
  return (
    <div>
      <p className="section-label">Similarity Ranking</p>
      <table className="ranking-table">
        <thead>
          <tr>
            <th>Rank</th>
            <th>Document A</th>
            <th>Document B</th>
            <th>Similarity</th>
            <th>Common Text</th>
          </tr>
        </thead>
        <tbody>
          {ranking.map((r) => {
            const key = `${r.docAId}-${r.docBId}`
            return (
              <tr
                key={key}
                className={`ranking-row${selectedPairKey === key ? ' selected' : ''}`}
                onClick={() => onSelectPair(key)}
              >
                <td>#{r.rank}</td>
                <td>{r.docAName}</td>
                <td>{r.docBName}</td>
                <td style={{ width: 160 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <div className="similarity-bar-track" style={{ flex: 1 }}>
                      <div className="similarity-bar-fill" style={{ width: `${Math.min(100, r.similarityPercent)}%` }} />
                    </div>
                    <span className="similarity-value">{r.similarityPercent.toFixed(2)}%</span>
                  </div>
                </td>
                <td>{r.sharedWords} words</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
