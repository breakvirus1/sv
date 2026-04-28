import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Navbar from './components/Navbar'
import LoginPage from './pages/LoginPage'
import Dashboard from './pages/Dashboard'
import OrdersList from './pages/OrdersList'
import OrderDetail from './pages/OrderDetail'
import CallbackPage from './pages/CallbackPage'

function App() {
  const { user, loading } = useAuth()

  if (loading) {
    return <div className="container">Loading...</div>
  }

  const isAuthenticated = !!user
  const hasPermission = user?.roles?.some(role =>
    ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_PRODUCTION', 'ROLE_ACCOUNTANT'].includes(role)
  )

  return (
    <div>
      <Navbar />
      <div className="container">
        <Routes>
          <Route path="/" element={
            isAuthenticated ? <Dashboard /> : <Navigate to="/login" />
          } />
          <Route path="/login" element={
            isAuthenticated ? <Navigate to="/orders" /> : <LoginPage />
          } />
          <Route path="/callback" element={<CallbackPage />} />
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          } />
          <Route path="/orders" element={
            <ProtectedRoute>
              <OrdersList />
            </ProtectedRoute>
          } />
          <Route path="/orders/new" element={
            <ProtectedRoute requiresManager={true}>
              <OrderDetail mode="create" />
            </ProtectedRoute>
          } />
          <Route path="/orders/:id" element={
            <ProtectedRoute>
              <OrderDetail />
            </ProtectedRoute>
          } />
        </Routes>
      </div>
    </div>
  )
}

const ProtectedRoute = ({ children, requiresManager }) => {
  const { user } = useAuth()

  if (!user) return <Navigate to="/login" />

  if (requiresManager && !(user.roles?.includes('ROLE_MANAGER') || user.roles?.includes('ROLE_ADMIN'))) {
    return <Navigate to="/orders" />
  }

  return children
}

export default App