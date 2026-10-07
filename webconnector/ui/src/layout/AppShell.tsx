import { useState, type ReactNode } from 'react'
import { NavLink, Outlet } from 'react-router'
import { Activity, Boxes, Coins, LogIn, LogOut, Menu, Moon, Sun, Upload, Users, X } from 'lucide-react'
import { cn } from '../lib/cn'
import { useTheme } from '../lib/theme'
import { useAuth } from '../lib/auth'
import { useNodeVersion } from '../lib/queries'
import { Button } from '../components/ui'
import { LoginDialog } from '../components/LoginDialog'

const nav = [
  { to: '/status', label: 'Node status', icon: Activity },
  { to: '/services', label: 'Services', icon: Boxes },
  { to: '/publish', label: 'Publish service', icon: Upload },
  { to: '/agents', label: 'Agents & groups', icon: Users },
  { to: '/wallet', label: 'Wallet & reputation', icon: Coins },
]

function Logo() {
  return (
    <div className="flex items-center gap-2.5">
      <img src={`${import.meta.env.BASE_URL}favicon.svg`} alt="" className="size-8" />
      <div className="leading-tight">
        <div className="font-semibold tracking-tight">las2peer</div>
        <NodeVersion />
      </div>
    </div>
  )
}

function NodeVersion() {
  const { data } = useNodeVersion()
  return <div className="text-xs text-zinc-500 dark:text-zinc-400">{data ? `node v${data}` : 'node'}</div>
}

function NavItems({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <ul className="space-y-1">
      {nav.map(({ to, label, icon: Icon }) => (
        <li key={to}>
          <NavLink
            to={to}
            onClick={onNavigate}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-100'
                  : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900 dark:text-zinc-400 dark:hover:bg-zinc-800/70 dark:hover:text-zinc-100',
              )
            }
          >
            <Icon className="size-4.5 shrink-0" aria-hidden />
            {label}
          </NavLink>
        </li>
      ))}
    </ul>
  )
}

function Account() {
  const { agent, logout } = useAuth()
  const [loginOpen, setLoginOpen] = useState(false)
  if (!agent)
    return (
      <>
        <Button size="sm" onClick={() => setLoginOpen(true)}>
          <LogIn className="size-4" /> Sign in
        </Button>
        <LoginDialog open={loginOpen} onClose={() => setLoginOpen(false)} />
      </>
    )
  const name = agent.username || agent.email || 'Agent'
  return (
    <div className="flex items-center gap-3">
      <div className="hidden text-right sm:block">
        <div className="text-sm font-medium leading-tight">{name}</div>
        {agent.email && agent.username && <div className="text-xs text-zinc-500 dark:text-zinc-400">{agent.email}</div>}
      </div>
      <div className="grid size-8 place-items-center rounded-full bg-brand-600 text-sm font-semibold text-white" aria-hidden>
        {name.slice(0, 1).toUpperCase()}
      </div>
      <Button variant="ghost" size="sm" onClick={() => void logout()} aria-label="Sign out">
        <LogOut className="size-4" />
      </Button>
    </div>
  )
}

function ThemeToggle() {
  const { theme, toggle } = useTheme()
  return (
    <Button variant="ghost" size="sm" onClick={toggle} aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}>
      {theme === 'dark' ? <Sun className="size-4" /> : <Moon className="size-4" />}
    </Button>
  )
}

export function AppShell({ children }: { children?: ReactNode }) {
  const [menuOpen, setMenuOpen] = useState(false)
  return (
    <div className="min-h-dvh lg:grid lg:grid-cols-[16rem_1fr]">
      {/* desktop sidebar */}
      <aside className="sticky top-0 hidden h-dvh flex-col border-r border-zinc-200 bg-white px-4 py-5 lg:flex dark:border-zinc-800 dark:bg-zinc-900">
        <Logo />
        <nav className="mt-8 flex-1" aria-label="Main">
          <NavItems />
        </nav>
        <p className="px-3 text-xs text-zinc-400">Decentralized community services</p>
      </aside>

      {/* mobile drawer */}
      {menuOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-zinc-950/40" onClick={() => setMenuOpen(false)} />
          <aside className="absolute inset-y-0 left-0 flex w-72 flex-col bg-white px-4 py-5 shadow-xl dark:bg-zinc-900">
            <div className="flex items-center justify-between">
              <Logo />
              <Button variant="ghost" size="sm" onClick={() => setMenuOpen(false)} aria-label="Close menu">
                <X className="size-4" />
              </Button>
            </div>
            <nav className="mt-8" aria-label="Main">
              <NavItems onNavigate={() => setMenuOpen(false)} />
            </nav>
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-col">
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between gap-4 border-b border-zinc-200 bg-white/80 px-4 backdrop-blur sm:px-6 dark:border-zinc-800 dark:bg-zinc-950/80">
          <div className="flex items-center gap-2 lg:hidden">
            <Button variant="ghost" size="sm" onClick={() => setMenuOpen(true)} aria-label="Open menu">
              <Menu className="size-5" />
            </Button>
            <Logo />
          </div>
          <div className="hidden lg:block" />
          <div className="flex items-center gap-2">
            <ThemeToggle />
            <Account />
          </div>
        </header>
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">{children ?? <Outlet />}</main>
      </div>
    </div>
  )
}
