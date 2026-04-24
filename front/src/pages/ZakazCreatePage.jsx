import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'

const ZakazCreatePage = () => {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    const formData = new FormData(e.target)

    try {
      await api.post('/api/zakaz/new', {
        userId: user?.sub || user?.id
      })
      navigate('/zakaz')
    } catch (err) {
      setError('Failed to create order')
    }
  }

  return (
    <div>
      <h1>Create Order</h1>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="card">
        <form onSubmit={handleSubmit}>
          <button type="submit" className="btn btn-success">
            Create Order
          </button>
        </form>
      </div>
    </div>
  )
}

export default ZakazCreatePage