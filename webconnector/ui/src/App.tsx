import { Navigate, Route, Routes } from 'react-router'
import { AppShell } from './layout/AppShell'
import { StatusPage } from './pages/StatusPage'
import { ServicesPage } from './pages/ServicesPage'
import { PublishPage } from './pages/PublishPage'
import { AgentsPage } from './pages/AgentsPage'
import { WalletPage } from './pages/WalletPage'
import { NotFoundPage } from './pages/NotFoundPage'

export function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/status" replace />} />
        {/* old frontend routes, kept so bookmarks keep working */}
        <Route path="welcome" element={<Navigate to="/status" replace />} />
        <Route path="view-services" element={<Navigate to="/services" replace />} />
        <Route path="publish-service" element={<Navigate to="/publish" replace />} />
        <Route path="agent-tools" element={<Navigate to="/agents" replace />} />
        <Route path="eth-tools" element={<Navigate to="/wallet" replace />} />
        <Route path="status" element={<StatusPage />} />
        <Route path="services" element={<ServicesPage />} />
        <Route path="publish" element={<PublishPage />} />
        <Route path="agents" element={<AgentsPage />} />
        <Route path="wallet" element={<WalletPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
