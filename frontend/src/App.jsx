import { useEffect, useState } from 'react'
import './App.css'

const DASHBOARD_API_URL = 'http://localhost:8080/api/account/1/dashboard'
const CASH_TRANSACTION_API_URL = 'http://localhost:8080/api/cash-transactions'
const ACCOUNT_ID = 1

function formatCurrency(value) {
  return `${Number(value).toLocaleString()} won`
}

function formatRate(value) {
  return `${Number(value).toFixed(2)}%`
}

function App() {
  const [dashboard, setDashboard] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isCashModalOpen, setIsCashModalOpen] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [formMessage, setFormMessage] = useState('')
  const [cashForm, setCashForm] = useState({
    type: 'DEPOSIT',
    amount: '',
    memo: '',
  })

  const loadDashboard = () => {
    setIsLoading(true)

    return fetch(DASHBOARD_API_URL)
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
  }

  useEffect(() => {
    loadDashboard()
  }, [])

  const handleCashFormChange = (event) => {
    const { name, value } = event.target

    setCashForm((previousForm) => ({
      ...previousForm,
      [name]: value,
    }))
  }

  const handleCashTransactionSubmit = (event) => {
    event.preventDefault()
    setIsSubmitting(true)
    setFormMessage('')

    fetch(CASH_TRANSACTION_API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        accountId: ACCOUNT_ID,
        type: cashForm.type,
        amount: Number(cashForm.amount),
        memo: cashForm.memo,
      }),
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error('Failed to save cash transaction.')
        }

        return response.json()
      })
      .then(() => {
        setCashForm({
          type: 'DEPOSIT',
          amount: '',
          memo: '',
        })
        setFormMessage('')
        setIsCashModalOpen(false)
        return loadDashboard()
      })
      .catch((error) => {
        setFormMessage(error.message)
      })
      .finally(() => {
        setIsSubmitting(false)
      })
  }

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
        <div className="header-actions">
          <div className="return-summary">
            <span>Total Return</span>
            <strong>{formatRate(dashboard.totalReturnRate)}</strong>
          </div>
          <button
            type="button"
            className="cash-action-button"
            onClick={() => {
              setFormMessage('')
              setIsCashModalOpen(true)
            }}
          >
            Cash Transaction
          </button>
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

      {isCashModalOpen && (
        <div className="modal-backdrop" role="presentation">
          <section className="modal" role="dialog" aria-modal="true" aria-labelledby="cash-modal-title">
            <div className="modal-header">
              <div>
                <h2 id="cash-modal-title">Cash Transaction</h2>
                <p>Deposit or withdraw cash from this account.</p>
              </div>
              <button
                type="button"
                className="modal-close-button"
                onClick={() => setIsCashModalOpen(false)}
                aria-label="Close cash transaction modal"
              >
                X
              </button>
            </div>

            <form className="cash-form" onSubmit={handleCashTransactionSubmit}>
              <label>
                Type
                <select name="type" value={cashForm.type} onChange={handleCashFormChange}>
                  <option value="DEPOSIT">Deposit</option>
                  <option value="WITHDRAWAL">Withdrawal</option>
                </select>
              </label>

              <label>
                Amount
                <input
                  type="number"
                  name="amount"
                  min="1"
                  value={cashForm.amount}
                  onChange={handleCashFormChange}
                  placeholder="1000000"
                  required
                />
              </label>

              <label>
                Memo
                <input
                  type="text"
                  name="memo"
                  value={cashForm.memo}
                  onChange={handleCashFormChange}
                  placeholder="Initial deposit"
                />
              </label>

              {formMessage && <p className="form-message">{formMessage}</p>}

              <div className="modal-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setIsCashModalOpen(false)}
                >
                  Cancel
                </button>
                <button type="submit" className="primary-button" disabled={isSubmitting}>
                  {isSubmitting ? 'Saving...' : 'Save'}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
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
