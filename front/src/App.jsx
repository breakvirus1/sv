import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Navbar from './components/Navbar'
import LoginPage from './pages/LoginPage'
import Dashboard from './pages/Dashboard'
import OrdersList from './pages/OrdersList'
import OrderDetail from './pages/OrderDetail'
import CallbackPage from './pages/CallbackPage'
import AdminPanel from './pages/AdminPanel'
import TestCalculations from './pages/TestCalculations'
import CreateOrderForm from './components/CreateOrderForm'

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
    <div className="relative min-h-screen bg-gradient-to-br from-[#0a5c8a] via-[#1a8cbf] to-[#0a5c8a]">
      {/* Desktop with windows */}
      <div className="relative min-h-screen">
        {/* Desktop icons area (optional) */}
        


        {/* Navbar fixed at top */}
        <div className="relative" style={{ zIndex: 10000 }}>
          <Navbar />
        </div>

        {/* Page content as "desktop icons" or background */}
        <div className="container pt-20">
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
                <CreateOrderForm />
              </ProtectedRoute>
            } />
            <Route path="/orders/:id" element={
              <ProtectedRoute>
                <OrderDetail />
              </ProtectedRoute>
            } />
            <Route path="/orders/:id/edit" element={
              <ProtectedRoute requiresManager={true}>
                <OrderDetail mode="edit" />
              </ProtectedRoute>
            } />
             <Route path="/admin" element={
               <ProtectedRoute requiresAdmin={true}>
                 <AdminPanel />
               </ProtectedRoute>
             } />
             <Route path="/test-calculations" element={
               <ProtectedRoute>
                 <TestCalculations />
               </ProtectedRoute>
             } />
           </Routes>
        </div>
      </div>
    </div>
  )
}

  const ProtectedRoute = ({ children, requiresManager, requiresAdmin }) => {
    const { user } = useAuth()

    if (!user) return <Navigate to="/login" />

    if (requiresManager && !(user.roles?.includes('ROLE_MANAGER') || user.roles?.includes('ROLE_ADMIN'))) {
      return <Navigate to="/orders" />
    }

    if (requiresAdmin && !user.roles?.includes('ROLE_ADMIN')) {
      return <Navigate to="/dashboard" />
    }

    return children
  }

export default App