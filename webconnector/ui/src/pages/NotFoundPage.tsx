import { Link } from 'react-router'
import { Compass } from 'lucide-react'
import { Card, EmptyState } from '../components/ui'

export function NotFoundPage() {
  return (
    <Card>
      <EmptyState icon={<Compass className="size-8" />} title="Page not found">
        <Link to="/status" className="font-medium text-brand-600 hover:underline">
          Back to node status
        </Link>
      </EmptyState>
    </Card>
  )
}
