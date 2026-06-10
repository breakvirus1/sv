import { useState, useEffect } from 'react'
import api from '../services/api'

const UsersPage = () => {
  const [users, setUsers] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    fetchUsers()
  }, [])

   const fetchUsers = async () => {
     try {
       const { data } = await api.get('/api/v1/employees?size=100')
       setUsers(data.content || [])
     } catch (err) {
       setError('Failed to fetch users')
     }
   }

  return (
    <div>
      <h1>Users Management</h1>
      {error && <div className="alert alert-danger">{error}</div>}
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Email</th>
            <th>Roles</th>
          </tr>
        </thead>
        <tbody>
          {users.map(user => (
            <tr key={user.id}>
              <td>{user.id}</td>
              <td>{user.username}</td>
              <td>{user.email}</td>
              <td>{user.roles?.join(', ')}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default UsersPage