import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="mx-auto max-w-xl px-4 py-16 text-center">
      <h1 className="font-display text-3xl font-semibold text-brand">Page not found</h1>
      <p className="mt-3 text-brand/85">The page you were looking for does not exist.</p>
      <Link
        to="/"
        className="btn mt-6 inline-block rounded-xl bg-brand px-5 py-3 font-semibold text-white"
      >
        Go to home
      </Link>
    </div>
  )
}
