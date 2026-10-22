import { useState } from 'react'
import api from '../services/api'

function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  async function handleLogin(event) {
    event.preventDefault()
    setError('')
    setSuccess(false)
    setLoading(true)

    try {
      const { data } = await api.post('/auth/login', { email, password })
      if (typeof data.token !== 'string' || !data.token) {
        throw new Error('Missing token')
      }
      localStorage.setItem('token', data.token)
      setPassword('')
      setSuccess(true)
    } catch (err) {
      setError(err.response?.status === 401
        ? 'Email ou mot de passe incorrect.'
        : 'Connexion impossible. Veuillez réessayer.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h2>Connexion</h2>
      <form onSubmit={handleLogin}>
        <div>
          <label htmlFor="email">Email :</label>
          <input
            id="email"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="password">Mot de passe :</label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </div>
        <button type="submit" disabled={loading}>
          {loading ? 'Connexion…' : 'Se connecter'}
        </button>
        {error && <p role="alert">{error}</p>}
        {success && <p role="status">Connexion réussie.</p>}
      </form>
    </div>
  )
}

export default Login
