import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AiStatusBanner } from './components/AiStatusBanner'
import { AppHeader } from './components/AppHeader'
import { AppProvider } from './context/AppContext'
import { Analyze } from './pages/Analyze'
import { Ask } from './pages/Ask'
import { Home } from './pages/Home'
import { Results } from './pages/Results'

export default function App() {
  return (
    <AppProvider>
      <BrowserRouter>
        <AiStatusBanner />
        <AppHeader />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/analyze/:mode" element={<Analyze />} />
          <Route path="/results" element={<Results />} />
          <Route path="/ask" element={<Ask />} />
        </Routes>
      </BrowserRouter>
    </AppProvider>
  )
}
