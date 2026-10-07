import { useMemo, useState } from 'react'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { Boxes, ChevronDown, Code2, ExternalLink, Play, Search, Square } from 'lucide-react'
import { Badge, Button, buttonClass, Card, Copyable, EmptyState, ErrorNote, Input, PageHeader, Spinner } from '../components/ui'
import { Stars } from '../components/Stars'
import { ApiError } from '../lib/api'
import { useAuth } from '../lib/auth'
import { formatDate, shortId } from '../lib/format'
import { useLocalNodeId, useRegistryServices, useServiceControl, type RegistryService, type ServiceRelease } from '../lib/queries'

/** Compares dotted versions numerically (1.10.0 > 1.9.2); non-numeric parts compare as text. */
function compareVersions(a: string, b: string): number {
  const pa = a.split('.')
  const pb = b.split('.')
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const x = pa[i] ?? '0'
    const y = pb[i] ?? '0'
    const nx = Number(x)
    const ny = Number(y)
    const diff = Number.isNaN(nx) || Number.isNaN(ny) ? x.localeCompare(y) : nx - ny
    if (diff) return diff
  }
  return 0
}

/** Service classes of a release: declared in its supplement, else whatever has been deployed. */
function classesOf(release: ServiceRelease): string[] {
  const declared = (release.supplement.class ?? '')
    .split(',')
    .map((c) => c.trim())
    .filter(Boolean)
  return declared.length ? declared : [...new Set(release.instances.map((i) => i.className))]
}

function ServiceCard({ service, localNodeId }: { service: RegistryService; localNodeId?: string }) {
  const { agent } = useAuth()
  const control = useServiceControl()
  const [open, setOpen] = useState(false)
  const versions = Object.keys(service.releases).sort(compareVersions).reverse()
  const [version, setVersion] = useState(versions[0])
  const release = service.releases[version]
  const classes = classesOf(release)
  const local = new Set(release.instances.filter((i) => i.nodeId === localNodeId).map((i) => i.className))
  const remoteNodes = new Set(release.instances.filter((i) => i.nodeId !== localNodeId).map((i) => i.nodeId))
  const notLocal = classes.filter((c) => !local.has(c))
  const displayName = release.supplement.name || service.name.split('.').pop()

  async function run(action: 'start' | 'stop', targets: string[]) {
    try {
      for (const cls of targets) await control.mutateAsync({ action, serviceName: `${service.name}.${cls}`, version })
      toast.success(`${action === 'start' ? 'Started' : 'Stopped'} ${displayName} ${version}`)
    } catch (err) {
      toast.error(`Could not ${action} ${displayName}`, { description: err instanceof Error ? err.message : String(err) })
    }
  }

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-4 px-5 py-4">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-base font-semibold">{displayName}</h2>
            {local.size > 0 && local.size === classes.length ? (
              <Badge tone="green">Running here</Badge>
            ) : local.size > 0 ? (
              <Badge tone="amber">Partly running here</Badge>
            ) : remoteNodes.size > 0 ? (
              <Badge tone="brand">Available in network</Badge>
            ) : (
              <Badge>Not running</Badge>
            )}
          </div>
          <p className="mt-0.5 font-mono text-xs text-zinc-500">{service.name}</p>
          {release.supplement.description && <p className="mt-2 text-sm text-zinc-600 dark:text-zinc-300">{release.supplement.description}</p>}
          <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-zinc-500 dark:text-zinc-400">
            <span className="inline-flex items-center gap-1.5">
              by <span className="font-medium text-zinc-700 dark:text-zinc-200">{service.authorName}</span>
              <Stars value={service.authorReputation} label="Author reputation" />
            </span>
            <span>published {formatDate(release.publicationEpochSeconds)}</span>
            <span>
              {local.size} of {classes.length || '?'} running here · {remoteNodes.size} other node{remoteNodes.size === 1 ? '' : 's'}
            </span>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {versions.length > 1 ? (
            <select
              value={version}
              onChange={(e) => setVersion(e.target.value)}
              aria-label="Version"
              className="h-8 rounded-lg bg-white px-2 text-sm ring-1 ring-zinc-300 dark:bg-zinc-950 dark:ring-zinc-700"
            >
              {versions.map((v) => (
                <option key={v} value={v}>
                  v{v}
                </option>
              ))}
            </select>
          ) : (
            <Badge>v{version}</Badge>
          )}
          {release.supplement.vcsUrl && (
            <a href={release.supplement.vcsUrl} target="_blank" rel="noreferrer" className={buttonClass('ghost', 'sm')}>
              <Code2 className="size-4" /> Source
            </a>
          )}
          {release.supplement.frontendUrl && (
            <a href={release.supplement.frontendUrl} target="_blank" rel="noreferrer" className={buttonClass('secondary', 'sm')}>
              <ExternalLink className="size-4" /> Open app
            </a>
          )}
          {local.size > 0 && (
            <Button variant="secondary" size="sm" disabled={!agent} loading={control.isPending} onClick={() => run('stop', [...local])}>
              <Square className="size-3.5" /> Stop
            </Button>
          )}
          {notLocal.length > 0 && (
            <Button
              size="sm"
              disabled={!agent}
              title={agent ? undefined : 'Sign in to start services'}
              loading={control.isPending}
              onClick={() => run('start', notLocal)}
            >
              <Play className="size-3.5" /> Start here
            </Button>
          )}
        </div>
      </div>
      {classes.length === 0 && (
        <p className="border-t border-zinc-200 px-5 py-2 text-xs text-amber-700 dark:border-zinc-800 dark:text-amber-400">
          This release does not declare its service classes, so it cannot be started from here.
        </p>
      )}
      {release.instances.length > 0 && (
        <div className="border-t border-zinc-200 dark:border-zinc-800">
          <button
            type="button"
            onClick={() => setOpen((o) => !o)}
            aria-expanded={open}
            className="flex w-full items-center gap-2 px-5 py-2.5 text-left text-sm font-medium text-zinc-600 hover:bg-zinc-50 dark:text-zinc-300 dark:hover:bg-zinc-800/50"
          >
            <ChevronDown className={`size-4 transition-transform ${open ? 'rotate-180' : ''}`} />
            {release.instances.length} deployment{release.instances.length === 1 ? '' : 's'}
          </button>
          {open && (
            <ul className="divide-y divide-zinc-100 px-5 pb-3 text-sm dark:divide-zinc-800">
              {release.instances.map((inst) => (
                <li key={inst.nodeId + inst.className} className="flex flex-wrap items-center justify-between gap-2 py-2">
                  <div className="min-w-0">
                    <span className="font-medium">{inst.className}</span>
                    {inst.nodeId === localNodeId && (
                      <Badge tone="green">
                        <span className="sr-only">on </span>this node
                      </Badge>
                    )}
                    <div className="text-xs text-zinc-500">
                      <Copyable value={inst.nodeId} display={shortId(inst.nodeId)} /> · hosted by {inst.nodeInfo['admin-name'] || 'unknown'}
                    </div>
                  </div>
                  <div className="flex items-center gap-3 text-xs text-zinc-500">
                    <Stars value={inst.hosterReputation} label="Hoster reputation" />
                    announced {formatDate(inst.announcementEpochSeconds)}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </Card>
  )
}

export function ServicesPage() {
  const services = useRegistryServices()
  const nodeId = useLocalNodeId()
  const [query, setQuery] = useState('')
  const noRegistry = services.error instanceof ApiError && services.error.status === 404

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    const list = [...(services.data ?? [])].sort((a, b) => a.name.localeCompare(b.name))
    if (!q) return list
    return list.filter((s) =>
      [s.name, s.authorName, ...Object.values(s.releases).flatMap((r) => [r.supplement.name, r.supplement.description])]
        .filter(Boolean)
        .some((t) => t!.toLowerCase().includes(q)),
    )
  }, [services.data, query])

  return (
    <>
      <PageHeader
        title="Services"
        description="Services published to the network's blockchain registry. Start one on this node, or see where it already runs."
        action={
          <Link to="/publish" className={buttonClass()}>
            Publish a service
          </Link>
        }
      />
      {services.data && services.data.length > 0 && (
        <div className="relative mb-4 max-w-sm">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-zinc-400" />
          <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search services, authors…" className="pl-9" aria-label="Search services" />
        </div>
      )}
      {services.isPending && <Spinner label="Reading the registry" />}
      {noRegistry ? (
        <Card>
          <EmptyState icon={<Boxes className="size-8" />} title="This node has no blockchain registry">
            Services are listed from the Ethereum registry. Start the node with an Ethereum configuration to browse them, or see the services
            running on this node on the <Link to="/status" className="text-brand-600 hover:underline">status page</Link>.
          </EmptyState>
        </Card>
      ) : (
        <ErrorNote error={services.error} />
      )}
      {services.data && (
        <div className="space-y-4">
          {filtered.map((s) => (
            <ServiceCard key={s.name} service={s} localNodeId={nodeId.data} />
          ))}
          {filtered.length === 0 && (
            <Card>
              <EmptyState icon={<Boxes className="size-8" />} title={query ? 'No matching services' : 'No services published yet'}>
                {query ? 'Try another search.' : 'Be the first: publish a service jar to the registry.'}
              </EmptyState>
            </Card>
          )}
        </div>
      )}
    </>
  )
}
