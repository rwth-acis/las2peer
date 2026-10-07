import { useState, type FormEvent } from 'react'
import { toast } from 'sonner'
import { Download, FileUp, Plus, Trash2, UserPlus, Users } from 'lucide-react'
import { Badge, Button, Card, CardBody, CardHeader, Copyable, ErrorNote, Field, Input, PageHeader } from '../components/ui'
import { SignInRequired } from '../components/SignInRequired'
import { useAuth } from '../lib/auth'
import { cn } from '../lib/cn'
import { shortId } from '../lib/format'
import { changeGroup, createAgent, createGroup, exportAgent, findAgent, loadGroup, uploadAgent, type AgentInfo } from '../lib/queries'

/** Looks up an agent by username, email or id (whatever the user typed). */
function lookup(term: string) {
  const t = term.trim()
  if (/^[0-9a-f]{64,}$/i.test(t)) return findAgent({ agentid: t })
  if (t.includes('@')) return findAgent({ email: t })
  return findAgent({ username: t })
}

function MemberEditor({ members, onChange, self }: { members: AgentInfo[]; onChange: (m: AgentInfo[]) => void; self?: string }) {
  const [term, setTerm] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>()

  async function add(e: FormEvent) {
    e.preventDefault()
    if (!term.trim()) return
    setBusy(true)
    setError(undefined)
    try {
      const found = await lookup(term)
      if (members.some((m) => m.agentid === found.agentid)) toast.info(`${found.username ?? 'Agent'} is already a member`)
      else onChange([...members, { agentid: found.agentid, username: found.username, email: found.email }])
      setTerm('')
    } catch {
      setError(new Error(`No agent found for “${term.trim()}”`))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="space-y-3">
      <form onSubmit={add} className="flex gap-2">
        <Input value={term} onChange={(e) => setTerm(e.target.value)} placeholder="Username, email or agent ID" aria-label="Add member" />
        <Button type="submit" variant="secondary" loading={busy}>
          <UserPlus className="size-4" /> Add
        </Button>
      </form>
      <ErrorNote error={error} />
      <ul className="divide-y divide-zinc-100 rounded-lg ring-1 ring-zinc-200 dark:divide-zinc-800 dark:ring-zinc-800">
        {members.map((m) => (
          <li key={m.agentid} className="flex items-center justify-between gap-3 px-3 py-2">
            <div className="min-w-0">
              <div className="flex items-center gap-2 text-sm font-medium">
                {m.username || m.email || 'Unnamed agent'}
                {m.agentid === self && <Badge tone="brand">you</Badge>}
              </div>
              <div className="text-xs text-zinc-500">
                <Copyable value={m.agentid} display={shortId(m.agentid)} />
              </div>
            </div>
            <Button
              variant="ghost"
              size="sm"
              aria-label={`Remove ${m.username ?? m.agentid}`}
              disabled={m.agentid === self}
              title={m.agentid === self ? 'You need to stay a member to manage the group' : undefined}
              onClick={() => onChange(members.filter((x) => x.agentid !== m.agentid))}
            >
              <Trash2 className="size-4" />
            </Button>
          </li>
        ))}
        {members.length === 0 && <li className="px-3 py-4 text-center text-sm text-zinc-500">No members yet</li>}
      </ul>
    </div>
  )
}

function CreateGroupCard() {
  const { agent } = useAuth()
  const me: AgentInfo = { agentid: agent!.agentid, username: agent!.username, email: agent!.email }
  const [members, setMembers] = useState<AgentInfo[]>([me])
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>()

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(undefined)
    try {
      const res = await createGroup(name.trim(), members)
      toast.success(`Group “${res.groupName}” created`, { description: `${members.length} members` })
      setName('')
      setMembers([me])
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card>
      <CardHeader title="Create a group" description="Groups are agents too: members share access to the group's data and identity." />
      <CardBody>
        <form id="create-group" onSubmit={submit}>
          <Field label="Group name">
            <Input value={name} onChange={(e) => setName(e.target.value)} required minLength={3} placeholder="my-team" />
          </Field>
        </form>
        <div className="mt-5">
          <span className="text-sm font-medium">Members</span>
          <div className="mt-1.5">
            <MemberEditor members={members} onChange={setMembers} self={agent!.agentid} />
          </div>
        </div>
        <div className="mt-4 space-y-3">
          <ErrorNote error={error} />
          <div className="flex justify-end">
            <Button type="submit" form="create-group" loading={busy} disabled={!name.trim()}>
              <Plus className="size-4" /> Create group
            </Button>
          </div>
        </div>
      </CardBody>
    </Card>
  )
}

function ManageGroupCard() {
  const { agent } = useAuth()
  const [term, setTerm] = useState('')
  const [group, setGroup] = useState<{ agentid: string; name: string; members: AgentInfo[] }>()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>()

  async function load(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(undefined)
    try {
      const res = await loadGroup(term.trim())
      setGroup({ agentid: res.agentid, name: term.trim(), members: res.members })
    } catch (err) {
      setGroup(undefined)
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  async function save() {
    if (!group) return
    setBusy(true)
    setError(undefined)
    try {
      await changeGroup(group.agentid, group.members)
      toast.success(`Saved members of “${group.name}”`)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card>
      <CardHeader title="Manage a group" description="Load a group you are a member of to change its members." />
      <CardBody className="space-y-4">
        <form onSubmit={load} className="flex gap-2">
          <Input value={term} onChange={(e) => setTerm(e.target.value)} placeholder="Group name or ID" aria-label="Group name or ID" required />
          <Button type="submit" variant="secondary" loading={busy && !group}>
            Load
          </Button>
        </form>
        <ErrorNote error={error} />
        {group && (
          <>
            <div className="text-xs text-zinc-500">
              Group ID <Copyable value={group.agentid} display={shortId(group.agentid)} />
            </div>
            <MemberEditor members={group.members} onChange={(members) => setGroup({ ...group, members })} self={agent!.agentid} />
            <div className="flex justify-end">
              <Button onClick={save} loading={busy}>
                Save members
              </Button>
            </div>
          </>
        )}
      </CardBody>
    </Card>
  )
}

function AgentToolsCard() {
  const [busy, setBusy] = useState<string>()

  async function run(key: string, fn: () => Promise<void>) {
    setBusy(key)
    try {
      await fn()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(undefined)
    }
  }

  function onCreate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const data = new FormData(form)
    void run('create', async () => {
      const res = await createAgent({
        username: String(data.get('username')).trim(),
        email: String(data.get('email') ?? '').trim() || undefined,
        password: String(data.get('password')),
      })
      toast.success(`Agent ${res.username} created`, { description: shortId(res.agentid) })
      form.reset()
    })
  }

  function onExport(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const term = String(new FormData(e.currentTarget).get('term')).trim()
    void run('export', async () => {
      const by = /^[0-9a-f]{64,}$/i.test(term) ? { agentid: term } : term.includes('@') ? { email: term } : { username: term }
      const blob = await exportAgent(by)
      const url = URL.createObjectURL(blob)
      const a = Object.assign(document.createElement('a'), { href: url, download: `agent-${term}.xml` })
      a.click()
      URL.revokeObjectURL(url)
      toast.success('Agent file downloaded', { description: 'The private key inside is encrypted with the agent’s password.' })
    })
  }

  function onImport(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const data = new FormData(form)
    const file = data.get('file')
    if (!(file instanceof File) || !file.size) return
    void run('import', async () => {
      const res = await uploadAgent(file, String(data.get('password') ?? ''))
      toast.success('Agent imported into the network', { description: shortId(res.agentid) })
      form.reset()
    })
  }

  return (
    <Card>
      <CardHeader title="Agent tools" description="Create agents for others, back up agent files, or bring an existing agent into this network." />
      <div className="grid divide-y divide-zinc-200 lg:grid-cols-3 lg:divide-x lg:divide-y-0 dark:divide-zinc-800">
        <form onSubmit={onCreate} className="space-y-3 p-5">
          <h3 className="font-medium">Create agent</h3>
          <Input name="username" placeholder="Username" required minLength={4} aria-label="Username" />
          <Input name="email" type="email" placeholder="Email (optional)" aria-label="Email" />
          <Input name="password" type="password" placeholder="Password" required minLength={8} aria-label="Password" autoComplete="new-password" />
          <Button type="submit" variant="secondary" className="w-full" loading={busy === 'create'}>
            <UserPlus className="size-4" /> Create
          </Button>
        </form>
        <form onSubmit={onExport} className="space-y-3 p-5">
          <h3 className="font-medium">Export agent</h3>
          <p className="text-sm text-zinc-500">Download an agent as an XML file.</p>
          <Input name="term" placeholder="Username, email or agent ID" required aria-label="Agent to export" />
          <Button type="submit" variant="secondary" className="w-full" loading={busy === 'export'}>
            <Download className="size-4" /> Download
          </Button>
        </form>
        <form onSubmit={onImport} className="space-y-3 p-5">
          <h3 className="font-medium">Import agent</h3>
          <input
            name="file"
            type="file"
            accept=".xml"
            required
            aria-label="Agent XML file"
            className={cn(
              'block w-full text-sm text-zinc-500 file:mr-3 file:rounded-md file:border-0 file:bg-zinc-100 file:px-3 file:py-1.5 file:text-sm file:font-medium',
              'dark:file:bg-zinc-800 dark:file:text-zinc-200',
            )}
          />
          <Input name="password" type="password" placeholder="Password (to verify, optional)" aria-label="Agent password" />
          <Button type="submit" variant="secondary" className="w-full" loading={busy === 'import'}>
            <FileUp className="size-4" /> Import
          </Button>
        </form>
      </div>
    </Card>
  )
}

export function AgentsPage() {
  const { agent } = useAuth()
  const [tab, setTab] = useState<'groups' | 'tools'>('groups')
  return (
    <>
      <PageHeader title="Agents & groups" description="Every user, group and service in las2peer is an agent with its own key pair." />
      <div className="mb-6 flex gap-1 border-b border-zinc-200 dark:border-zinc-800" role="tablist">
        {(
          [
            ['groups', 'Groups', Users],
            ['tools', 'Agent tools', UserPlus],
          ] as const
        ).map(([key, label, Icon]) => (
          <button
            key={key}
            role="tab"
            aria-selected={tab === key}
            onClick={() => setTab(key)}
            className={cn(
              '-mb-px inline-flex items-center gap-2 border-b-2 px-3 py-2 text-sm font-medium',
              tab === key ? 'border-brand-600 text-brand-700 dark:text-brand-100' : 'border-transparent text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-100',
            )}
          >
            <Icon className="size-4" /> {label}
          </button>
        ))}
      </div>
      {tab === 'groups' ? (
        agent ? (
          <div className="grid gap-6 lg:grid-cols-2">
            <CreateGroupCard />
            <ManageGroupCard />
          </div>
        ) : (
          <SignInRequired what="create and manage groups" />
        )
      ) : (
        <AgentToolsCard />
      )}
    </>
  )
}
