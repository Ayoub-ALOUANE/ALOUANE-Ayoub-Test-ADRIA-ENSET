import { useCallback, useEffect, useState } from 'react'
import { api, API_BASE } from './api'
import './App.css'

const TABS = [
  { id: 'accounts', label: 'Accounts' },
  { id: 'transactions', label: 'Transactions' },
  { id: 'transfer', label: 'Transfer' },
  { id: 'chat', label: 'AI Assistant' },
]

function App() {
  const [tab, setTab] = useState('accounts')
  const [accounts, setAccounts] = useState([])
  const [transactions, setTransactions] = useState([])
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [loading, setLoading] = useState(false)

  const [newAccount, setNewAccount] = useState({
    customerReference: '',
    currency: 'EUR',
    balance: 1000,
    status: 'ACTIVE',
  })
  const [txForm, setTxForm] = useState({
    accountId: '',
    amount: 50,
    currency: 'EUR',
    merchantCategory: 'ECOMMERCE',
    country: 'FR',
  })
  const [transferForm, setTransferForm] = useState({
    fromAccountId: '',
    toAccountId: '',
    amount: 100,
    currency: 'EUR',
    country: 'FR',
  })
  const [chatInput, setChatInput] = useState('list accounts')
  const [messages, setMessages] = useState([
    { role: 'bot', text: 'Banking agent ready. Try “list accounts” or “help”.' },
  ])

  const refresh = useCallback(async () => {
    setError('')
    try {
      const [accs, txs] = await Promise.all([api.getAccounts(), api.getTransactions()])
      setAccounts(accs || [])
      setTransactions(txs || [])
    } catch (e) {
      setError(e.message + ` (API: ${API_BASE})`)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  async function createAccount(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    setOk('')
    try {
      await api.createAccount({
        ...newAccount,
        balance: Number(newAccount.balance),
      })
      setOk('Account created')
      setNewAccount((s) => ({ ...s, customerReference: '' }))
      await refresh()
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function createTx(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    setOk('')
    try {
      await api.createTransaction({
        ...txForm,
        amount: Number(txForm.amount),
      })
      setOk('Transaction recorded — Kafka event published')
      await refresh()
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function doTransfer(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    setOk('')
    try {
      await api.transfer({
        ...transferForm,
        amount: Number(transferForm.amount),
      })
      setOk('Transfer completed')
      await refresh()
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function sendChat(e) {
    e.preventDefault()
    const text = chatInput.trim()
    if (!text) return
    setMessages((m) => [...m, { role: 'user', text }])
    setChatInput('')
    setLoading(true)
    try {
      const res = await api.chat(text)
      setMessages((m) => [
        ...m,
        {
          role: 'bot',
          text: res.reply || 'Done',
          detail: res.toolResult ? JSON.stringify(res.toolResult, null, 2) : null,
        },
      ])
      await refresh()
    } catch (err) {
      setMessages((m) => [...m, { role: 'bot', text: 'Error: ' + err.message }])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <div className="brand-mark">
            Adria<span>Ledger</span>
          </div>
          <div className="brand-sub">Banking microservices console · Gateway {API_BASE}</div>
        </div>
        <nav className="tabs">
          {TABS.map((t) => (
            <button
              key={t.id}
              className={`tab ${tab === t.id ? 'active' : ''}`}
              onClick={() => setTab(t.id)}
            >
              {t.label}
            </button>
          ))}
        </nav>
      </header>

      {error && <div className="alert">{error}</div>}
      {ok && <div className="toast-ok">{ok}</div>}

      {tab === 'accounts' && (
        <section className="panel">
          <h2>Accounts</h2>
          <p className="hint">REST · GraphQL · MCP share the same AccountService</p>
          <div className="grid grid-2">
            <div className="grid">
              {accounts.length === 0 && <p className="muted">No accounts yet.</p>}
              {accounts.map((a) => (
                <article key={a.id} className="account-card">
                  <div className="ref">{a.customerReference}</div>
                  <div className="meta uuid">{a.id}</div>
                  <div className="balance">
                    {Number(a.balance).toLocaleString(undefined, { minimumFractionDigits: 2 })}{' '}
                    {a.currency}
                  </div>
                  <span className={`badge ${a.status === 'ACTIVE' ? 'active' : 'blocked'}`}>
                    {a.status}
                  </span>
                </article>
              ))}
            </div>
            <form className="form" onSubmit={createAccount}>
              <h3 style={{ margin: 0 }}>Open account</h3>
              <label>
                Customer reference
                <input
                  required
                  value={newAccount.customerReference}
                  onChange={(e) => setNewAccount({ ...newAccount, customerReference: e.target.value })}
                />
              </label>
              <label>
                Currency
                <select
                  value={newAccount.currency}
                  onChange={(e) => setNewAccount({ ...newAccount, currency: e.target.value })}
                >
                  <option>EUR</option>
                  <option>USD</option>
                  <option>MAD</option>
                </select>
              </label>
              <label>
                Initial balance
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={newAccount.balance}
                  onChange={(e) => setNewAccount({ ...newAccount, balance: e.target.value })}
                />
              </label>
              <button className="btn" disabled={loading}>
                Create account
              </button>
              <button type="button" className="btn secondary" onClick={refresh}>
                Refresh
              </button>
            </form>
          </div>
        </section>
      )}

      {tab === 'transactions' && (
        <section className="panel">
          <h2>Transactions</h2>
          <p className="hint">Max amount from Config Server: bank.transaction.max-amount=1000</p>
          <div className="grid grid-2">
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Amount</th>
                    <th>Account</th>
                    <th>Category</th>
                    <th>When</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((t) => (
                    <tr key={t.id}>
                      <td>
                        {t.amount} {t.currency}
                      </td>
                      <td className="uuid">{t.accountId?.slice(0, 8)}…</td>
                      <td>{t.merchantCategory || '—'}</td>
                      <td>{t.createdAt ? new Date(t.createdAt).toLocaleString() : '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <form className="form" onSubmit={createTx}>
              <h3 style={{ margin: 0 }}>New transaction</h3>
              <label>
                Account
                <select
                  required
                  value={txForm.accountId}
                  onChange={(e) => setTxForm({ ...txForm, accountId: e.target.value })}
                >
                  <option value="">Select…</option>
                  {accounts
                    .filter((a) => a.status === 'ACTIVE')
                    .map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.customerReference} ({a.currency})
                      </option>
                    ))}
                </select>
              </label>
              <label>
                Amount
                <input
                  type="number"
                  min="0.01"
                  max="1000"
                  step="0.01"
                  value={txForm.amount}
                  onChange={(e) => setTxForm({ ...txForm, amount: e.target.value })}
                />
              </label>
              <label>
                Merchant category
                <input
                  value={txForm.merchantCategory}
                  onChange={(e) => setTxForm({ ...txForm, merchantCategory: e.target.value })}
                />
              </label>
              <label>
                Country
                <input
                  value={txForm.country}
                  onChange={(e) => setTxForm({ ...txForm, country: e.target.value })}
                />
              </label>
              <button className="btn" disabled={loading}>
                Debit account
              </button>
            </form>
          </div>
        </section>
      )}

      {tab === 'transfer' && (
        <section className="panel">
          <h2>Transfer</h2>
          <p className="hint">Virement entre deux comptes actifs (même devise)</p>
          <form className="form" onSubmit={doTransfer} style={{ maxWidth: 480 }}>
            <label>
              From
              <select
                required
                value={transferForm.fromAccountId}
                onChange={(e) => setTransferForm({ ...transferForm, fromAccountId: e.target.value })}
              >
                <option value="">Select…</option>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.customerReference} — {a.balance} {a.currency}
                  </option>
                ))}
              </select>
            </label>
            <label>
              To
              <select
                required
                value={transferForm.toAccountId}
                onChange={(e) => setTransferForm({ ...transferForm, toAccountId: e.target.value })}
              >
                <option value="">Select…</option>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.customerReference} — {a.balance} {a.currency}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Amount
              <input
                type="number"
                min="0.01"
                max="1000"
                step="0.01"
                value={transferForm.amount}
                onChange={(e) => setTransferForm({ ...transferForm, amount: e.target.value })}
              />
            </label>
            <button className="btn" disabled={loading}>
              Execute transfer
            </button>
          </form>
        </section>
      )}

      {tab === 'chat' && (
        <section className="panel">
          <h2>AI Assistant</h2>
          <p className="hint">Agentic chatbot calling MCP tools on account &amp; transaction services</p>
          <div className="chat-log">
            {messages.map((m, i) => (
              <div key={i} className={`bubble ${m.role}`}>
                <div>{m.text}</div>
                {m.detail && <pre>{m.detail}</pre>}
              </div>
            ))}
          </div>
          <form className="form" onSubmit={sendChat}>
            <label>
              Message
              <input
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                placeholder="list accounts"
              />
            </label>
            <button className="btn" disabled={loading}>
              Send
            </button>
          </form>
        </section>
      )}
    </div>
  )
}

export default App
