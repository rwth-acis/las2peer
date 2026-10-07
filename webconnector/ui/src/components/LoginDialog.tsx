import { useState, type FormEvent } from 'react'
import { toast } from 'sonner'
import { useAuth } from '../lib/auth'
import { cn } from '../lib/cn'
import { Button, Dialog, ErrorNote, Field, Input } from './ui'

export function LoginDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { login, register } = useAuth()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>()

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const data = new FormData(e.currentTarget)
    const username = String(data.get('username') ?? '').trim()
    const password = String(data.get('password') ?? '')
    setBusy(true)
    setError(undefined)
    try {
      const agent =
        mode === 'login'
          ? await login(username, password)
          : await register({ username, password, email: String(data.get('email') ?? '').trim() || undefined })
      toast.success(mode === 'login' ? `Signed in as ${agent.username ?? username}` : `Welcome, ${agent.username ?? username}!`)
      onClose()
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} title={mode === 'login' ? 'Sign in to this node' : 'Create an account'}>
      <div className="mb-4 grid grid-cols-2 rounded-lg bg-zinc-100 p-1 text-sm font-medium dark:bg-zinc-800" role="tablist">
        {(['login', 'register'] as const).map((m) => (
          <button
            key={m}
            type="button"
            role="tab"
            aria-selected={mode === m}
            onClick={() => {
              setMode(m)
              setError(undefined)
            }}
            className={cn(
              'rounded-md py-1.5 transition-colors',
              mode === m ? 'bg-white shadow-sm dark:bg-zinc-950' : 'text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-100',
            )}
          >
            {m === 'login' ? 'Sign in' : 'Create account'}
          </button>
        ))}
      </div>
      <form className="space-y-4" onSubmit={submit}>
        <Field label={mode === 'login' ? 'Username, email or agent ID' : 'Username'}>
          <Input name="username" required minLength={mode === 'register' ? 4 : 1} autoComplete="username" autoFocus />
        </Field>
        {mode === 'register' && (
          <Field label="Email" hint="Optional. Can also be used to sign in.">
            <Input name="email" type="email" autoComplete="email" />
          </Field>
        )}
        <Field
          label="Password"
          hint={mode === 'register' ? 'Encrypts your agent’s private key. It cannot be recovered.' : undefined}
        >
          <Input
            name="password"
            type="password"
            required
            minLength={mode === 'register' ? 8 : 1}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          />
        </Field>
        <ErrorNote error={error} />
        <Button type="submit" className="w-full" loading={busy}>
          {mode === 'login' ? 'Sign in' : 'Create account'}
        </Button>
      </form>
    </Dialog>
  )
}
