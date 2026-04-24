import { useAuth } from '../context/AuthContext'

const LoginPage = () => {
  const { login } = useAuth()

  const handleLogin = () => {
    login()
  }

  return (
    <div className="container" style={{ maxWidth: '400px', marginTop: '100px' }}>
      <div className="card">
        <h2>Login</h2>
        <p>Click the button below to login via OAuth2</p>
        <button onClick={handleLogin} className="btn" style={{ width: '100%' }}>
          Login
        </button>
      </div>
    </div>
  )
}

export default LoginPage