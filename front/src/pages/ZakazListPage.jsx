import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'

const ZakazListPage = () => {
  const { user } = useAuth()
  const [zakazs, setZakazs] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    fetchZakazs()
  }, [user])

   const fetchZakazs = async () => {
     try {
       const response = await api.get(`/api/zakaz/all?id=${user?.sub || user?.id}`)
       setZakazs(response.data)
     } catch (err) {
       setError('Failed to fetch orders')
     }
   }

  return (
    <div>
      <h1>Orders</h1>
      {error && <div className="alert alert-danger">{error}</div>}
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Sum</th>
            <th>Created At</th>
          </tr>
        </thead>
        <tbody>
          {zakazs.map(zakaz => (
            <tr key={zakaz.id}>
              <td>{zakaz.id}</td>
              <td>{zakaz.sum}</td>
              <td>{new Date(zakaz.createdAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default ZakazListPage