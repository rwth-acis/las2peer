import { Cpu, ExternalLink, HardDrive, MemoryStick, Network, Server } from 'lucide-react'
import type { ReactNode } from 'react'
import { Badge, Card, CardBody, CardHeader, Copyable, EmptyState, ErrorNote, PageHeader, Spinner, Stat, Table, Td } from '../components/ui'
import { shortId } from '../lib/format'
import { useHasRegistry, useNodeStatus, useOtherNodes } from '../lib/queries'
import { cn } from '../lib/cn'

function Meter({ icon, label, percent, detail }: { icon: ReactNode; label: string; percent?: number; detail?: string }) {
  const known = percent !== undefined && Number.isFinite(percent) && percent >= 0
  const value = known ? Math.min(100, Math.max(0, Math.round(percent))) : 0
  const tone = value > 85 ? 'bg-red-500' : value > 65 ? 'bg-amber-500' : 'bg-brand-500'
  return (
    <Card>
      <CardBody>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-sm font-medium text-zinc-600 dark:text-zinc-300">
            {icon}
            {label}
          </div>
          <span className="text-2xl font-semibold tabular-nums">{known ? `${value}%` : '–'}</span>
        </div>
        <div
          className="mt-3 h-2 overflow-hidden rounded-full bg-zinc-100 dark:bg-zinc-800"
          role="meter"
          aria-label={label}
          aria-valuenow={known ? value : undefined}
          aria-valuemin={0}
          aria-valuemax={100}
        >
          <div className={cn('h-full rounded-full transition-[width] duration-500', tone)} style={{ width: `${value}%` }} />
        </div>
        <p className="mt-2 text-xs text-zinc-500 dark:text-zinc-400">{detail || (known ? '' : 'Not available on this node')}</p>
      </CardBody>
    </Card>
  )
}

export function StatusPage() {
  const status = useNodeStatus()
  const others = useOtherNodes()
  const registry = useHasRegistry()
  const s = status.data

  return (
    <>
      <PageHeader
        title="Node status"
        description="Live resource usage of this node, the services it runs, and the peers it knows in the las2peer network."
        action={
          registry.data !== undefined && (
            <Badge tone={registry.data ? 'green' : 'neutral'}>{registry.data ? 'Ethereum registry connected' : 'No blockchain registry'}</Badge>
          )
        }
      />
      {status.isPending && <Spinner label="Loading node status" />}
      <ErrorNote error={status.error} />
      {s && (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-3">
            <Meter icon={<Cpu className="size-4" />} label="CPU" percent={s.cpuLoad} />
            <Meter
              icon={<MemoryStick className="size-4" />}
              label="Memory"
              percent={(s.ramLoad / s.maxRamLoad) * 100}
              detail={`${s.ramLoadStr} of ${s.maxRamLoadStr}`}
            />
            <Meter
              icon={<HardDrive className="size-4" />}
              label="Local storage"
              percent={s.storageSize >= 0 && s.maxStorageSize > 0 ? (s.storageSize / s.maxStorageSize) * 100 : undefined}
              detail={s.storageSize >= 0 ? `${s.storageSizeStr} of ${s.maxStorageSizeStr}` : undefined}
            />
          </div>

          <Card>
            <CardHeader title="This node" description={s.nodeDescription} action={<Badge tone="green">Online · up {s.uptime}</Badge>} />
            <CardBody>
              <dl className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
                <Stat label="Node ID" value={<Copyable value={s.nodeId} display={shortId(s.nodeId, 10, 6)} />} />
                <Stat label="Operator" value={s.nodeAdminName || '–'} hint={s.nodeAdminEmail} />
                <Stat label="Organization" value={s.nodeOrganization || '–'} />
                <Stat label="Services running" value={s.localServices?.length ?? 0} />
              </dl>
            </CardBody>
          </Card>

          <Card>
            <CardHeader title="Services on this node" description="Each service exposes a REST API; open its OpenAPI description to explore it." />
            {s.localServices?.length ? (
              <Table head={['Service', 'Version', 'API']}>
                {s.localServices.map((svc) => (
                  <tr key={`${svc.name}@${svc.version}`}>
                    <Td className="font-medium">{svc.name}</Td>
                    <Td>
                      <Badge>{svc.version}</Badge>
                    </Td>
                    <Td>
                      {svc.swagger ? (
                        <a className="inline-flex items-center gap-1 text-brand-600 hover:underline" href={svc.swagger} target="_blank" rel="noreferrer">
                          OpenAPI <ExternalLink className="size-3.5" />
                        </a>
                      ) : (
                        <span className="text-zinc-400">–</span>
                      )}
                    </Td>
                  </tr>
                ))}
              </Table>
            ) : (
              <EmptyState icon={<Server className="size-8" />} title="No services running">
                Start one from the Services page.
              </EmptyState>
            )}
          </Card>

          <Card>
            <CardHeader
              title="Known nodes"
              description="Peers in this node's neighbourhood. Their service lists are self-reported, not verified by the registry."
            />
            {others.isPending ? (
              <CardBody>
                <Spinner label="Asking peers" />
              </CardBody>
            ) : others.data?.length ? (
              <Table head={['Node', 'Operator', 'Services']}>
                {others.data.map((n) => (
                  <tr key={n.nodeID}>
                    <Td>
                      <div className="font-mono text-xs">{n.nodeID}</div>
                      {n.nodeInfo.description && <div className="text-xs text-zinc-500">{n.nodeInfo.description}</div>}
                    </Td>
                    <Td>{n.nodeInfo['admin-name'] || '–'}</Td>
                    <Td>
                      <div className="flex flex-wrap gap-1">
                        {n.nodeInfo.services?.length
                          ? n.nodeInfo.services.map((svc) => (
                              <Badge key={svc['service-name'] + svc['service-version']}>
                                {svc['service-name'].split('.').pop()} {svc['service-version']}
                              </Badge>
                            ))
                          : '–'}
                      </div>
                    </Td>
                  </tr>
                ))}
              </Table>
            ) : (
              <EmptyState icon={<Network className="size-8" />} title="No other nodes yet">
                This node is alone in its network. Start another node with this one as bootstrap to see it here.
              </EmptyState>
            )}
          </Card>
        </div>
      )}
    </>
  )
}
