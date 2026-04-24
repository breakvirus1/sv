import { useAuth } from '../context/AuthContext'

const Dashboard = () => {
  const { user } = useAuth()

  return (
    <div>
      <h1>Dashboard</h1>
      <div className="card">
        <h3>Welcome, {user?.username}!</h3>
        <p><strong>Roles:</strong> {user?.roles?.join(', ')}</p>
      </div>
    </div>
  )
}

export default Dashboard