import { useRef, useState } from 'react'

const money = (value) => `${Number(value).toLocaleString()} won`
const tone = (value) => value > 0 ? 'number-positive' : value < 0 ? 'number-negative' : ''
const reasons = {
  INSUFFICIENT_CASH: 'Insufficient virtual cash',
  INSUFFICIENT_QUANTITY: 'Insufficient virtual shares',
}

export default function PlanSimulationView({ plan, actions, onBack, onExecuted }) {
  const [result, setResult] = useState(null)
  const [running, setRunning] = useState(false)
  const [error, setError] = useState('')
  const [warning, setWarning] = useState('')
  const busy = useRef(false)

  async function run() {
    if (busy.current) return
    busy.current = true
    setRunning(true)
    setError('')
    setWarning('')
    try {
      const response = await fetch(`http://localhost:8080/api/plans/${plan.id}/simulation`, { method: 'POST' })
      const body = await response.json().catch(() => null)
      if (!response.ok) throw new Error(body?.message || 'Simulation failed. Please try again.')
      if (!body || body.planId !== plan.id) throw new Error('Invalid simulation response.')
      setResult(body)
      try {
        await onExecuted(plan.id)
      } catch {
        setWarning('Simulation saved, but action statuses could not be refreshed.')
      }
    } catch (failure) {
      setError(failure.message)
    } finally {
      busy.current = false
      setRunning(false)
    }
  }

  return <section className="simulation-view" aria-busy={running}>
    <div className="simulation-heading">
      <div><h2>{plan.stockName} Simulation</h2><p>{plan.stockSymbol} / Plan #{plan.id} / {plan.planStatus}</p></div>
      <div className="simulation-controls">
        <button type="button" className="secondary-button" disabled={running} onClick={onBack}>Back to Plans</button>
        <button type="button" className="primary-button" disabled={running} onClick={run}>
          {running ? 'Running...' : result ? 'Run Again' : 'Run Simulation'}
        </button>
      </div>
    </div>
    {running && <p role="status">Calculating virtual executions...</p>}
    {error && <p role="alert" className="form-message">{error}</p>}
    {warning && <p role="alert" className="form-message">{warning}</p>}
    {!result ? <p className="table-empty-state">No simulation result loaded.</p> : <>
      <div className="section-heading"><h3>Cumulative Virtual Position</h3><span>State as of {result.processedThrough}</span></div>
      {error && <p role="status">Previous successful result</p>}
      <div className="summary-grid simulation-summary">
        {[
          ['Virtual Cash', money(result.cash)], ['Shares', result.quantity.toLocaleString()],
          ['Remaining Cost Basis', money(result.costBasis)], ['Average Price', money(result.averagePrice)],
          ['Realized Profit / Loss', money(result.realizedProfit), tone(result.realizedProfit)],
        ].map(([label, value, color]) => <div className="summary-item" key={label}>
          <span>{label}</span><strong className={color}>{value}</strong>
        </div>)}
      </div>
      <h3>New Executions / This Run</h3>
      {result.executions.length === 0 ? <p>No new executions in this run.</p> :
        <div className="performance-table-scroll"><table>
          <thead><tr>{['Date', 'Action', 'Type', 'Price', 'Shares', 'Amount', 'Sold Cost', 'Realized P/L'].map(label => <th key={label}>{label}</th>)}</tr></thead>
          <tbody>{result.executions.map(item => <tr key={item.actionId}>
            <td>{item.date}</td><td>#{item.actionId}</td><td className={item.type === 'SELL' ? 'trade-sell' : ''}>{item.type}</td>
            <td>{money(item.price)}</td><td>{item.quantity}</td><td>{money(item.amount)}</td>
            <td>{item.type === 'SELL' ? money(item.costBasis) : '-'}</td>
            <td className={tone(item.realizedProfit)}>{item.type === 'SELL' ? money(item.realizedProfit) : '-'}</td>
          </tr>)}</tbody>
        </table></div>}
      <h3>Skipped Actions / This Run</h3>
      {result.skippedActions.length === 0 ? <p>No skipped actions in this run.</p> :
        <div className="performance-table-scroll"><table>
          <thead><tr><th>Date</th><th>Action</th><th>Reason</th></tr></thead>
          <tbody>{result.skippedActions.map(item => <tr key={`${item.actionId}-${item.date}`}>
            <td>{item.date}</td><td>#{item.actionId}</td><td>{reasons[item.reason] || item.reason}</td>
          </tr>)}</tbody>
        </table></div>}
    </>}
    <h3>Plan Actions</h3>
    {actions.length === 0 ? <p>No actions.</p> : <div className="performance-table-scroll"><table>
      <thead><tr>{['Action', 'Type', 'Target Price', 'Shares', 'Actual Status', 'Virtual Status', 'Virtual Date'].map(label => <th key={label}>{label}</th>)}</tr></thead>
      <tbody>{actions.map(action => <tr key={action.id}>
        <td>#{action.id}</td><td className={action.actionType === 'SELL' ? 'trade-sell' : ''}>{action.actionType}</td><td>{money(action.triggerPrice)}</td><td>{action.quantity}</td>
        <td>{action.actionStatus}</td><td>{action.simulationStatus || 'PENDING'}</td><td>{action.executedDate || '-'}</td>
      </tr>)}</tbody>
    </table></div>}
  </section>
}
