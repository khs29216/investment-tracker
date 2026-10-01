import { useEffect, useState } from 'react'

const money = (value) => `${Number(value).toLocaleString()} won`
const rate = (value) => `${Number(value).toFixed(1)}%`
const tone = (value) => value > 0 ? 'number-positive' : value < 0 ? 'number-negative' : ''

export default function PerformanceView({ accountId }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    fetch(`http://localhost:8080/api/account/${accountId}/performance`, { signal: controller.signal })
      .then(async (response) => {
        const body = await response.json()
        if (!response.ok) throw new Error(body.message || 'Failed to load performance.')
        return body
      })
      .then((body) => { if (!controller.signal.aborted) setData(body) })
      .catch((failure) => { if (!controller.signal.aborted) setError(failure.message) })
    return () => controller.abort()
  }, [accountId, attempt])

  if (error) return <section role="alert"><p>{error}</p><button type="button" onClick={() => {
    setError('')
    setAttempt((value) => value + 1)
  }}>Retry</button></section>
  if (!data) return <p role="status">Loading performance...</p>

  return (
    <div className="performance-view">
      <section className="summary-grid" aria-label="Realized performance summary">
        {[
          ['Sell Amount', money(data.totalSellAmount)],
          ['Sold Cost Basis', money(data.realizedCostBasis)],
          ['Realized Profit / Loss', money(data.realizedProfit), tone(data.realizedProfit)],
          ['Realized Return', rate(data.realizedReturnRate), tone(data.realizedReturnRate)],
        ].map(([label, value, color]) => <div className="summary-item" key={label}>
          <span>{label}</span><strong className={color}>{value}</strong>
        </div>)}
      </section>
      <section>
        <h2>Performance by Stock</h2>
        {data.stocks.length === 0 ? <p>No sell transactions yet.</p> : (
          <div className="performance-table-scroll"><table>
            <thead><tr>{['Stock', 'Symbol', 'Sell Amount', 'Cost Basis', 'Profit / Loss', 'Return'].map((label) => <th key={label}>{label}</th>)}</tr></thead>
            <tbody>{data.stocks.map((stock) => <tr key={stock.stockSymbol}>
              <td>{stock.stockName}</td><td>{stock.stockSymbol}</td>
              <td>{money(stock.totalSellAmount)}</td><td>{money(stock.realizedCostBasis)}</td>
              <td className={tone(stock.realizedProfit)}>{money(stock.realizedProfit)}</td>
              <td className={tone(stock.realizedReturnRate)}>{rate(stock.realizedReturnRate)}</td>
            </tr>)}</tbody>
          </table></div>
        )}
      </section>
      <section>
        <h2>Realized Trade History</h2>
        {data.realizedTrades.length === 0 ? <p>No sell transactions yet.</p> : (
          <div className="performance-table-scroll"><table>
            <thead><tr>{['Date', 'Stock', 'Symbol', 'Quantity', 'Sell Price', 'Sell Amount', 'Cost Basis', 'Profit / Loss', 'Return'].map((label) => <th key={label}>{label}</th>)}</tr></thead>
            <tbody>{data.realizedTrades.map((trade) => <tr key={trade.tradeId}>
              <td>{trade.tradeDateTime.replace('T', ' ')}</td>
              <td>{trade.stockName}</td><td>{trade.stockSymbol}</td><td>{trade.quantity}</td>
              <td>{money(trade.tradePrice)}</td><td>{money(trade.sellAmount)}</td><td>{money(trade.realizedCostBasis)}</td>
              <td className={tone(trade.realizedProfit)}>{money(trade.realizedProfit)}</td>
              <td className={tone(trade.realizedReturnRate)}>{rate(trade.realizedReturnRate)}</td>
            </tr>)}</tbody>
          </table></div>
        )}
      </section>
    </div>
  )
}
