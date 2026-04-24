import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const CallbackPage = () => {
  const { handleCallback } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const code = params.get('code')
    const state = params.get('state')

    if (code && state) {
      handleCallback(code, state)
        .then(() => navigate('/'))
        .catch(() => navigate('/login'))
    } else {
      navigate('/login')
    }
  }, [])

  return <div>Processing login...</div>
}

export default CallbackPage