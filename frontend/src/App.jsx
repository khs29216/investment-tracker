import { useEffect, useState } from 'react'
import './App.css'

const DASHBOARD_API_URL = 'http://localhost:8080/api/account/1/dashboard'
const CASH_TRANSACTION_API_URL = 'http://localhost:8080/api/cash-transactions'
const CASH_TRANSACTION_LIST_API_URL = 'http://localhost:8080/api/accounts/1/cash-transactions'
const TRADE_API_URL = 'http://localhost:8080/api/trades'
const ACCOUNT_ID = 1

function formatCurrency(value) {
  return `${Number(value).toLocaleString()} won`
}

function formatCashTransactionAmount(transaction) {
  const prefix = transaction.type === 'WITHDRAWAL' ? '-' : ''

  return `${prefix}${formatCurrency(transaction.amount)}`
}

function formatRate(value) {
  return `${Number(value).toFixed(2)}%`
}

function formatDateTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function getNumberToneClassName(value) {
  const numberValue = Number(value)

  if (numberValue > 0) {
    return 'number-positive'
  }

  if (numberValue < 0) {
    return 'number-negative'
  }

  return ''
}

function formatLocalDateTime(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

function App() {
  const [dashboard, setDashboard] = useState(null)
  const [trades, setTrades] = useState([])
  const [cashTransactions, setCashTransactions] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isCashModalOpen, setIsCashModalOpen] = useState(false)
  const [isTradeModalOpen, setIsTradeModalOpen] = useState(false)
  const [isTradeHistoryModalOpen, setIsTradeHistoryModalOpen] = useState(false)
  const [isCashHistoryModalOpen, setIsCashHistoryModalOpen] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [formMessage, setFormMessage] = useState('')
  const [tradeMessage, setTradeMessage] = useState('')
  const [cashForm, setCashForm] = useState({
    type: 'DEPOSIT',
    amount: '',
    memo: '',
  })
  const [tradeForm, setTradeForm] = useState({
    tradeType: 'BUY',
    stockName: '',
    stockSymbol: '',
    tradePrice: '',
    quantity: '',
    memo: '',
  })

  const loadPageData = () => {
    setIsLoading(true)

    return Promise.all([
      fetchJson(DASHBOARD_API_URL),
      fetchJson(TRADE_API_URL),
      fetchJson(CASH_TRANSACTION_LIST_API_URL),
    ])
      .then(([dashboardData, tradeData, cashTransactionData]) => {
        setDashboard(dashboardData)
        setTrades(
          tradeData
            .filter((trade) => trade.accountId === ACCOUNT_ID)
            .sort((a, b) => b.tradeDateTime.localeCompare(a.tradeDateTime))
            .slice(0, 5),
        )
        setCashTransactions(
          cashTransactionData
            .sort((a, b) => b.transactionDateTime.localeCompare(a.transactionDateTime))
            .slice(0, 5),
        )
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
    loadPageData()
  }, [])

  const handleCashFormChange = (event) => {
    const { name, value } = event.target

    setCashForm((previousForm) => ({
      ...previousForm,
      [name]: value,
    }))
  }

  const handleTradeFormChange = (event) => {
    const { name, value } = event.target

    setTradeForm((previousForm) => ({
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
        return loadPageData()
      })
      .catch((error) => {
        setFormMessage(error.message)
      })
      .finally(() => {
        setIsSubmitting(false)
      })
  }

  const handleTradeSubmit = (event) => {
    event.preventDefault()
    setIsSubmitting(true)
    setTradeMessage('')

    fetch(TRADE_API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        accountId: ACCOUNT_ID,
        stockName: tradeForm.stockName,
        stockSymbol: tradeForm.stockSymbol,
        tradeType: tradeForm.tradeType,
        tradePrice: Number(tradeForm.tradePrice),
        quantity: Number(tradeForm.quantity),
        tradeDateTime: formatLocalDateTime(new Date()),
        memo: tradeForm.memo,
        planActionId: null,
      }),
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error('Failed to save trade.')
        }

        return response.json()
      })
      .then(() => {
        setTradeForm({
          tradeType: 'BUY',
          stockName: '',
          stockSymbol: '',
          tradePrice: '',
          quantity: '',
          memo: '',
        })
        setTradeMessage('')
        setIsTradeModalOpen(false)
        return loadPageData()
      })
      .catch((error) => {
        setTradeMessage(error.message)
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
            <strong className={getNumberToneClassName(dashboard.totalReturnRate)}>
              {formatRate(dashboard.totalReturnRate)}
            </strong>
          </div>
          <button
            type="button"
            className="cash-action-button"
            onClick={() => {
              setTradeMessage('')
              setIsTradeModalOpen(true)
            }}
          >
            Trade
          </button>
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
        <SummaryItem
          label="Profit/Loss"
          value={formatCurrency(dashboard.totalProfitLoss)}
          valueClassName={getNumberToneClassName(dashboard.totalProfitLoss)}
        />
      </section>

      <section className="holdings-section">
        <div className="section-heading">
          <h2>Stock Holdings</h2>
          <span>{dashboard.stockHoldings.length} items</span>
        </div>

        {dashboard.stockHoldings.length > 0 ? (
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
                    <td className={getNumberToneClassName(stock.profitLoss)}>
                      {formatCurrency(stock.profitLoss)}
                    </td>
                    <td className={getNumberToneClassName(stock.returnRate)}>
                      {formatRate(stock.returnRate)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="table-empty-state">No stock holdings yet.</div>
        )}

        <div className="history-actions" aria-label="Account history actions">
          <button
            type="button"
            className="history-button"
            onClick={() => setIsTradeHistoryModalOpen(true)}
          >
            Trade History
          </button>
          <button
            type="button"
            className="history-button"
            onClick={() => setIsCashHistoryModalOpen(true)}
          >
            Cash History
          </button>
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

      {isTradeModalOpen && (
        <div className="modal-backdrop" role="presentation">
          <section className="modal" role="dialog" aria-modal="true" aria-labelledby="trade-modal-title">
            <div className="modal-header">
              <div>
                <h2 id="trade-modal-title">Trade</h2>
                <p>Record a buy or sell transaction.</p>
              </div>
              <button
                type="button"
                className="modal-close-button"
                onClick={() => setIsTradeModalOpen(false)}
                aria-label="Close trade modal"
              >
                X
              </button>
            </div>

            <form className="cash-form" onSubmit={handleTradeSubmit}>
              <label>
                Type
                <select name="tradeType" value={tradeForm.tradeType} onChange={handleTradeFormChange}>
                  <option value="BUY">Buy</option>
                  <option value="SELL">Sell</option>
                </select>
              </label>

              <label>
                Stock Name
                <input
                  type="text"
                  name="stockName"
                  value={tradeForm.stockName}
                  onChange={handleTradeFormChange}
                  placeholder="Samsung Electronics"
                  required
                />
              </label>

              <label>
                Stock Symbol
                <input
                  type="text"
                  name="stockSymbol"
                  value={tradeForm.stockSymbol}
                  onChange={handleTradeFormChange}
                  placeholder="005930"
                  required
                />
              </label>

              <label>
                Price
                <input
                  type="number"
                  name="tradePrice"
                  min="1"
                  value={tradeForm.tradePrice}
                  onChange={handleTradeFormChange}
                  placeholder="70000"
                  required
                />
              </label>

              <label>
                Quantity
                <input
                  type="number"
                  name="quantity"
                  min="1"
                  value={tradeForm.quantity}
                  onChange={handleTradeFormChange}
                  placeholder="10"
                  required
                />
              </label>

              <label>
                Memo
                <input
                  type="text"
                  name="memo"
                  value={tradeForm.memo}
                  onChange={handleTradeFormChange}
                  placeholder="Dashboard trade"
                />
              </label>

              {tradeMessage && <p className="form-message">{tradeMessage}</p>}

              <div className="modal-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setIsTradeModalOpen(false)}
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

      {isTradeHistoryModalOpen && (
        <div className="modal-backdrop" role="presentation">
          <section
            className="modal history-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="trade-history-modal-title"
          >
            <div className="modal-header">
              <div>
                <h2 id="trade-history-modal-title">Trade History</h2>
                <p>Recent buy and sell records for this account.</p>
              </div>
              <button
                type="button"
                className="modal-close-button"
                onClick={() => setIsTradeHistoryModalOpen(false)}
                aria-label="Close trade history modal"
              >
                X
              </button>
            </div>

            <ActivityPanel title="Recent Trades" emptyMessage="No trades yet.">
              {trades.map((trade) => (
                <div className="activity-row" key={trade.id}>
                  <div>
                    <strong>{trade.stockName}</strong>
                    <span>
                      <span className={trade.tradeType === 'SELL' ? 'trade-sell' : ''}>
                        {trade.tradeType}
                      </span>{' '}
                      · {trade.stockSymbol} · {formatDateTime(trade.tradeDateTime)}
                    </span>
                  </div>
                  <div className="activity-value">
                    {formatCurrency(trade.tradePrice)} × {trade.quantity}
                  </div>
                </div>
              ))}
            </ActivityPanel>
          </section>
        </div>
      )}

      {isCashHistoryModalOpen && (
        <div className="modal-backdrop" role="presentation">
          <section
            className="modal history-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="cash-history-modal-title"
          >
            <div className="modal-header">
              <div>
                <h2 id="cash-history-modal-title">Cash History</h2>
                <p>Recent deposit and withdrawal records for this account.</p>
              </div>
              <button
                type="button"
                className="modal-close-button"
                onClick={() => setIsCashHistoryModalOpen(false)}
                aria-label="Close cash history modal"
              >
                X
              </button>
            </div>

            <ActivityPanel title="Cash Transactions" emptyMessage="No cash transactions yet.">
              {cashTransactions.map((transaction) => (
                <div className="activity-row" key={transaction.id}>
                  <div>
                    <strong>{transaction.type}</strong>
                    <span>{formatDateTime(transaction.transactionDateTime)}</span>
                  </div>
                  <div
                    className={`activity-value ${
                      transaction.type === 'DEPOSIT' ? 'cash-deposit' : ''
                    }`}
                  >
                    {formatCashTransactionAmount(transaction)}
                  </div>
                </div>
              ))}
            </ActivityPanel>
          </section>
        </div>
      )}
    </main>
  )
}

function fetchJson(url) {
  return fetch(url).then((response) => {
    if (!response.ok) {
      throw new Error('Failed to load dashboard data.')
    }

    return response.json()
  })
}

function SummaryItem({ label, value, valueClassName = '' }) {
  return (
    <div className="summary-item">
      <span>{label}</span>
      <strong className={valueClassName}>{value}</strong>
    </div>
  )
}

function ActivityPanel({ title, emptyMessage, children }) {
  const hasItems = Array.isArray(children) ? children.length > 0 : Boolean(children)

  return (
    <section className="activity-panel">
      <div className="section-heading">
        <h2>{title}</h2>
      </div>
      {hasItems ? children : <p className="empty-list">{emptyMessage}</p>}
    </section>
  )
}

export default App
