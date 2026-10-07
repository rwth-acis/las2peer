import { useState } from 'react'
import { Lock } from 'lucide-react'
import { Button, Card, EmptyState } from './ui'
import { LoginDialog } from './LoginDialog'

export function SignInRequired({ what }: { what: string }) {
  const [open, setOpen] = useState(false)
  return (
    <Card>
      <EmptyState icon={<Lock className="size-8" />} title="Sign in required">
        <p>Sign in with your las2peer agent to {what}.</p>
        <Button className="mt-4" onClick={() => setOpen(true)}>
          Sign in or create account
        </Button>
      </EmptyState>
      <LoginDialog open={open} onClose={() => setOpen(false)} />
    </Card>
  )
}
