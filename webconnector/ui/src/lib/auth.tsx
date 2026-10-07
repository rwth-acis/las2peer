import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { request } from './api'

// The node identifies the user by the HttpOnly `sessionid` cookie that /auth/login sets.
// We only keep the agent's display data here (sessionStorage) to render the UI.

export interface SessionAgent {
  agentid: string
  username?: string
  email?: string
  ethaddress?: string
}

interface AuthState {
  agent?: SessionAgent
  ready: boolean
  login: (user: string, password: string) => Promise<SessionAgent>
  register: (fields: { username: string; email?: string; password: string }) => Promise<SessionAgent>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)
const STORAGE_KEY = 'las2peer-agent'

function load(): SessionAgent | undefined {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as SessionAgent) : undefined
  } catch {
    return undefined
  }
}

function save(agent: SessionAgent | undefined) {
  try {
    if (agent) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(agent))
    else sessionStorage.removeItem(STORAGE_KEY)
  } catch {
    // without storage the user just has to sign in again after a reload
  }
}

const basic = (user: string, password: string) => `Basic ${btoa(unescape(encodeURIComponent(`${user}:${password}`)))}`

export function AuthProvider({ children }: { children: ReactNode }) {
  const [agent, setAgent] = useState<SessionAgent | undefined>(load)
  const [ready, setReady] = useState(false)
  const queryClient = useQueryClient()

  const update = useCallback(
    (next: SessionAgent | undefined) => {
      save(next)
      setAgent(next)
      // everything user-specific must be refetched for the new identity
      void queryClient.invalidateQueries()
    },
    [queryClient],
  )

  // check that the session cookie is still valid (server restarts / 24h timeout end sessions)
  useEffect(() => {
    request<{ agentid?: string }>('/auth/validate')
      .then((res) => {
        if (!res?.agentid) update(undefined)
        else if (res.agentid !== load()?.agentid) update({ agentid: res.agentid })
      })
      .catch(() => update(undefined))
      .finally(() => setReady(true))
  }, [update])

  const login = useCallback(
    async (user: string, password: string) => {
      const res = await request<SessionAgent>('/auth/login', { headers: { Authorization: basic(user, password) } })
      const next = { agentid: res.agentid, username: res.username, email: res.email, ethaddress: res.ethaddress }
      update(next)
      return next
    },
    [update],
  )

  const register = useCallback(
    async (fields: { username: string; email?: string; password: string }) => {
      // creates the agent (with a fresh wallet on Ethereum nodes) and starts a session;
      // a mnemonic must not be sent unless the user brings one, "" would be used as the seed
      const res = await request<SessionAgent>('/auth/create', { body: fields })
      const next = { agentid: res.agentid, username: res.username, email: res.email, ethaddress: res.ethaddress }
      update(next)
      return next
    },
    [update],
  )

  const logout = useCallback(async () => {
    try {
      await request('/auth/logout', { method: 'POST' })
    } finally {
      update(undefined)
    }
  }, [update])

  const value = useMemo(() => ({ agent, ready, login, register, logout }), [agent, ready, login, register, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth outside AuthProvider')
  return ctx
}
