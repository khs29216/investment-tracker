import { useEffect, useState } from 'react'
import './App.css'

const DASHBOARD_API_URL = 'http://localhost:8080/api/account/1/dashboard'

function formatCurrency(value) {
  return `${Number(value).toLocaleString()} won`
}

function formatRate(value) {
  return `${Number(value).toFixed(2)}%`
}

function App() {
  const [dashboard, setDashboard] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    fetch(DASHBOARD_API_URL)
      .then((response) => {
        if (!response.ok) {
          throw new Error('Failed to load account dashboard.')
        }

        return response.json()
      })
      .then((data) => {
        setDashboard(data)
        setErrorMessage('')
      })
      .catch((error) => {
        setErrorMessage(error.message)
      })
      .finally(() => {
        setIsLoading(false)
      })
  }, [])

  if (isLoading) {
    return <main className="dashboard-page">Loading dashboard...</main>
  }

  if (errorMessage) {
    return (
      <main className="dashboard-page">
        <section className="empty-state">
          <h1>Account Dashboard</h1>
          <p>{errorMessage}</p>
        </section>
      </main>
    )
  }

  return (
    <main className="dashboard-page">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">Investment Tracker</p>
          <h1>{dashboard.accountName}</h1>
        </div>
        <div className="return-summary">
          <span>Total Return</span>
          <strong>{formatRate(dashboard.totalReturnRate)}</strong>
        </div>
      </header>

      <section className="summary-grid" aria-label="Account summary">
        <SummaryItem label="Cash Balance" value={formatCurrency(dashboard.cashBalance)} />
        <SummaryItem
          label="Investment Amount"
          value={formatCurrency(dashboard.totalInvestmentAmount)}
        />
        <SummaryItem
          label="Evaluation Amount"
          value={formatCurrency(dashboard.totalEvaluationAmount)}
        />
        <SummaryItem label="Profit/Loss" value={formatCurrency(dashboard.totalProfitLoss)} />
      </section>

      <section className="holdings-section">
        <div className="section-heading">
          <h2>Stock Holdings</h2>
          <span>{dashboard.stockHoldings.length} items</span>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Symbol</th>
                <th>Qty</th>
                <th>Avg Price</th>
                <th>Current Price</th>
                <th>Investment</th>
                <th>Evaluation</th>
                <th>Profit/Loss</th>
                <th>Return</th>
              </tr>
            </thead>
            <tbody>
              {dashboard.stockHoldings.map((stock) => (
                <tr key={stock.stockSymbol}>
                  <td>{stock.stockName}</td>
                  <td>{stock.stockSymbol}</td>
                  <td>{stock.quantity.toLocaleString()}</td>
                  <td>{formatCurrency(stock.averagePrice)}</td>
                  <td>{formatCurrency(stock.currentPrice)}</td>
                  <td>{formatCurrency(stock.investmentAmount)}</td>
                  <td>{formatCurrency(stock.evaluationAmount)}</td>
                  <td>{formatCurrency(stock.profitLoss)}</td>
                  <td>{formatRate(stock.returnRate)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </main>
  )
}

function SummaryItem({ label, value }) {
  return (
    <div className="summary-item">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

export default App
