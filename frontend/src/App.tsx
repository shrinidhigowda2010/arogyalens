import { lazy, Suspense } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AiStatusBanner } from './components/AiStatusBanner'
import { AppHeader } from './components/AppHeader'
import { SkipLink } from './components/SkipLink'
import { AppProvider } from './context/AppContext'
import Home from './pages/Home'

// Secondary routes are code-split so the first load stays small on slow mobile networks.
const Analyze = lazy(() => import('./pages/Analyze'))
const Results = lazy(() => import('./pages/Results'))
const Ask = lazy(() => import('./pages/Ask'))
const Doctors = lazy(() => import('./pages/Doctors'))
const History = lazy(() => import('./pages/History'))
const NotFound = lazy(() => import('./pages/NotFound'))

function PageFallback() {
  return (
    <p role="status" className="p-8 text-center text-brand/85">
      Loading…
    </p>
  )
}

export default function App() {
  return (
    <AppProvider>
      <BrowserRouter>
        <SkipLink />
        <AiStatusBanner />
        <AppHeader />
        <main id="main" tabIndex={-1} className="outline-none">
          <Suspense fallback={<PageFallback />}>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/analyze/:mode" element={<Analyze />} />
              <Route path="/results" element={<Results />} />
              <Route path="/ask" element={<Ask />} />
              <Route path="/doctors" element={<Doctors />} />
              <Route path="/history" element={<History />} />
              <Route path="*" element={<NotFound />} />
            </Routes>
          </Suspense>
        </main>
      </BrowserRouter>
    </AppProvider>
  )
}
