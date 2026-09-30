import { useEffect, useId, useState } from 'react'

export default function StockSearchInput({ stockName, stockSymbol, onSelect }) {
  const id = useId()
  const [keyword, setKeyword] = useState('')
  const [results, setResults] = useState([])
  const [status, setStatus] = useState('idle')
  const [message, setMessage] = useState('')
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    if (!keyword.trim() || stockSymbol) return
    const controller = new AbortController()
    const timer = setTimeout(async () => {
      try {
        const response = await fetch(
          `http://localhost:8080/api/stocks/search?keyword=${encodeURIComponent(keyword.trim())}`,
          { signal: controller.signal },
        )
        if (!response.ok) {
          const error = await response.json().catch(() => ({}))
          throw new Error(error.message || 'Stock search is unavailable.')
        }
        const stocks = await response.json()
        if (controller.signal.aborted) return
        setResults(stocks)
        setStatus('success')
      } catch (error) {
        if (controller.signal.aborted) return
        setMessage(error.message)
        setStatus('error')
      }
    }, 300)
    return () => {
      clearTimeout(timer)
      controller.abort()
    }
  }, [keyword, stockSymbol, retry])

  return (
    <div className="stock-search">
      <label htmlFor={id}>Stock</label>
      <input
        id={id}
        type="search"
        value={stockSymbol ? stockName : keyword}
        placeholder="삼성전자 / 005930"
        autoComplete="off"
        required
        onChange={(event) => {
          const value = event.target.value
          setKeyword(value)
          setResults([])
          setStatus(value.trim() ? 'loading' : 'idle')
          onSelect({ stockName: '', stockSymbol: '' })
        }}
      />
      {!stockSymbol && (
        <>
          <div className="stock-search-status" role="status">
            {status === 'loading' && 'Searching...'}
            {status === 'success' && results.length === 0 && 'No stocks found.'}
            {status === 'error' && message}
          </div>
          {status === 'error' && (
            <button type="button" onClick={() => {
              setStatus('loading')
              setRetry((value) => value + 1)
            }}>Retry</button>
          )}
          {results.length > 0 && (
            <ul className="stock-search-results" aria-label="Stock search results">
              {results.map((stock) => (
                <li key={`${stock.market}-${stock.stockSymbol}`}>
                  <button type="button" onClick={() => {
                    onSelect(stock)
                    setKeyword('')
                    setResults([])
                    setStatus('idle')
                  }}>
                    <strong>{stock.stockName}</strong>
                    <span>{stock.stockSymbol} · {stock.market}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
      <label htmlFor={`${id}-symbol`}>Stock Symbol</label>
      <input id={`${id}-symbol`} value={stockSymbol} readOnly placeholder="-" />
    </div>
  )
}
