import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Navbar from './components/Navbar'
import LoginPage from './pages/LoginPage'
import Dashboard from './pages/Dashboard'
import UsersPage from './pages/UsersPage'
import ZakazListPage from './pages/ZakazListPage'
import ZakazCreatePage from './pages/ZakazCreatePage'
import CallbackPage from './pages/CallbackPage'

function App() {
  const { user, loading } = useAuth()

  if (loading) {
    return <div className="container">Loading...</div>
  }

  const isAuthenticated = !!user
  const isOperator = user?.roles?.includes('ROLE_OPERATOR') || user?.roles?.includes('ROLE_ADMIN')
  const isAdmin = user?.roles?.includes('ROLE_ADMIN')

  return (
    <div>
      <Navbar />
      <div className="container">
        <Routes>
          <Route path="/" element={
            isAuthenticated ? <Dashboard /> : <Navigate to="/login" />
          } />
          <Route path="/login" element={
            isAuthenticated ? <Navigate to="/" /> : <LoginPage />
          } />
          <Route path="/callback" element={<CallbackPage />} />
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          } />
          <Route path="/users" element={
            <ProtectedRoute requiresAdmin={true}>
              <UsersPage />
            </ProtectedRoute>
          } />
          <Route path="/zakaz" element={
            <ProtectedRoute requiresOperator={true}>
              <ZakazListPage />
            </ProtectedRoute>
          } />
          <Route path="/zakaz/create" element={
            <ProtectedRoute requiresOperator={true}>
              <ZakazCreatePage />
            </ProtectedRoute>
          } />
        </Routes>
      </div>
    </div>
  )
}

const ProtectedRoute = ({ children, requiresAdmin, requiresOperator }) => {
  const { user } = useAuth()

  if (!user) return <Navigate to="/login" />

  if (requiresAdmin && !user.roles?.includes('ROLE_ADMIN')) {
    return <Navigate to="/" />
  }

  if (requiresOperator && !(user.roles?.includes('ROLE_OPERATOR') || user.roles?.includes('ROLE_ADMIN'))) {
    return <Navigate to="/" />
  }

  return children
}

export default App