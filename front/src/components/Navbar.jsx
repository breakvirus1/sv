import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const Navbar = () => {
  const { user, logout } = useAuth()

  return (
    <nav className="navbar">
      <Link to="/">PrintSV</Link>
      <div>
        {user ? (
          <>
            <span style={{ marginRight: '1rem' }}>
              {user.username} ({user.roles?.join(', ')})
            </span>
            <Link to="/dashboard">Dashboard</Link>
            {user.roles?.includes('ROLE_ADMIN') && (
              <Link to="/users">Users</Link>
            )}
            {(user.roles?.includes('ROLE_OPERATOR') || user.roles?.includes('ROLE_ADMIN')) && (
              <Link to="/zakaz">Orders</Link>
            )}
            <button onClick={logout} className="btn btn-danger" style={{ marginLeft: '1rem' }}>
              Logout
            </button>
          </>
        ) : (
          <button onClick={() => window.location.href = '/login'}>Login</button>
        )}
      </div>
    </nav>
  )
}

export default Navbar