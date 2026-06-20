import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Navbar from './components/Navbar'
import LoginPage from './pages/LoginPage'
import Dashboard from './pages/Dashboard'
import OrdersList from './pages/OrdersList'
import ManagerOrderList from './pages/ManagerOrderList'
import OrderDetail from './pages/OrderDetail'
import OrderItemDetail from './pages/OrderItemDetail'
import CallbackPage from './pages/CallbackPage'
import AdminPanel from './pages/AdminPanel'
import ProductionOrderList from './pages/ProductionOrderList'
import ProductionOrdersPositionsList from './pages/ProductionOrdersPositionsList'
import TestCalculations from './pages/TestCalculations'
import CreateOrderForm from './components/CreateOrderForm'
import ManagerOrderDetail from './pages/ManagerOrderDetail'
import ManagerDashboard from './pages/ManagerDashboard'

function App() {
  const { user, loading } = useAuth()

  if (loading) {
    return <div className="container">Loading...</div>
  }

  const isAuthenticated = !!user
  const isManager = user?.roles?.includes('ROLE_MANAGER') || user?.roles?.includes('ROLE_ADMIN')
  const isProduction = user?.roles?.includes('ROLE_PRODUCTION')
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
         <div className="pt-20" style={{ paddingLeft: 0, paddingRight: 0, maxWidth: '100%', margin: 0, width: '100%' }}>
          <Routes>
             <Route path="/" element={
               isAuthenticated ? (isManager ? <Navigate to="/manager" /> : isProduction ? <Navigate to="/production" /> : <Dashboard />) : <Navigate to="/login" />
             } />
             <Route path="/login" element={
               isAuthenticated ? (isManager ? <Navigate to="/manager" /> : isProduction ? <Navigate to="/production" /> : <Navigate to="/orders" />) : <LoginPage />
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
              <Route path="/production" element={
                <ProtectedRoute>
                  <ProductionOrderList />
                </ProtectedRoute>
              } />
             <Route path="/production/positions" element={
               <ProtectedRoute>
                 <ProductionOrdersPositionsList />
               </ProtectedRoute>
             } />
            <Route path="/manager" element={
              <ProtectedRoute requiresManager={true}>
                <ManagerOrderList />
              </ProtectedRoute>
            } />
            <Route path="/orders/new" element={
              <ProtectedRoute requiresManager={true}>
                <CreateOrderForm />
              </ProtectedRoute>
            } />
            <Route path="/orders/:orderId/items/:itemId" element={
              <ProtectedRoute>
                <OrderItemDetail />
              </ProtectedRoute>
            } />
            <Route path="/orders/:id" element={
              <ProtectedRoute>
                <OrderDetail />
              </ProtectedRoute>
            } />
            <Route path="/manager/orders/:id" element={
              <ProtectedRoute requiresManager={true}>
                <ManagerOrderDetail />
              </ProtectedRoute>
            } />
            <Route path="/manager/dashboard" element={
              <ProtectedRoute requiresManager={true}>
                <ManagerDashboard />
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
      if (user.roles?.includes('ROLE_PRODUCTION')) {
        return <Navigate to="/production" />
      }
      return <Navigate to="/orders" />
    }

    if (requiresAdmin && !user.roles?.includes('ROLE_ADMIN')) {
      return <Navigate to="/dashboard" />
    }

    return children
  }

export default App