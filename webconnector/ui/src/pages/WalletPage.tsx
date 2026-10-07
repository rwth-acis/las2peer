import { useState, type FormEvent } from 'react'
import { toast } from 'sonner'
import { ArrowDownLeft, ArrowUpRight, Coins, Eye, EyeOff, Gift, HandCoins, ShieldCheck, Sparkles, Wallet } from 'lucide-react'
import {
  Badge,
  Button,
  Card,
  CardBody,
  CardHeader,
  Copyable,
  Dialog,
  EmptyState,
  ErrorNote,
  Field,
  Input,
  PageHeader,
  Spinner,
  Table,
  Td,
  Textarea,
} from '../components/ui'
import { SignInRequired } from '../components/SignInRequired'
import { Stars } from '../components/Stars'
import { useAuth } from '../lib/auth'
import { cn } from '../lib/cn'
import { formatNumber, shortId } from '../lib/format'
import {
  parseRating,
  useCoinbase,
  useDashboard,
  useHasRegistry,
  useRateAgent,
  useRegisterProfile,
  useRequestFaucet,
  useSendCoins,
  useTxLog,
  useWallet,
  USER_RATING_TX,
  type DashboardAgent,
  type EthWallet,
  type FaucetResult,
  type TxLogEntry,
} from '../lib/queries'

const hasProfile = (w: EthWallet) =>
  w.agentHasProfile !== undefined ? Boolean(Number(w.agentHasProfile)) : w.ethCumulativeScore !== undefined && w.ethCumulativeScore !== '???'

function StatCard({ icon, label, value, hint }: { icon: React.ReactNode; label: string; value: React.ReactNode; hint?: React.ReactNode }) {
  return (
    <Card>
      <CardBody>
        <div className="flex items-center gap-2 text-sm text-zinc-500 dark:text-zinc-400">
          {icon}
          {label}
        </div>
        <div className="mt-2 text-2xl font-semibold tabular-nums">{value}</div>
        {hint && <div className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">{hint}</div>}
      </CardBody>
    </Card>
  )
}

function WalletCard({ wallet }: { wallet: EthWallet }) {
  const [reveal, setReveal] = useState(false)
  return (
    <Card>
      <CardHeader title="Your wallet" description="Created for your agent on this node. It holds your L2Pcoin and signs your reputation transactions." />
      <CardBody className="space-y-4">
        <div>
          <div className="text-xs font-medium uppercase tracking-wide text-zinc-500">Address</div>
          {wallet.ethAgentCredentialsAddress ? <Copyable value={wallet.ethAgentCredentialsAddress} /> : '–'}
        </div>
        {wallet.ethMnemonic && (
          <div>
            <div className="flex items-center justify-between">
              <div className="text-xs font-medium uppercase tracking-wide text-zinc-500">Recovery phrase</div>
              <Button variant="ghost" size="sm" onClick={() => setReveal((r) => !r)}>
                {reveal ? <EyeOff className="size-4" /> : <Eye className="size-4" />} {reveal ? 'Hide' : 'Reveal'}
              </Button>
            </div>
            {reveal ? (
              <div className="mt-1 rounded-lg bg-amber-50 p-3 dark:bg-amber-500/10">
                <Copyable value={wallet.ethMnemonic} className="text-amber-900 dark:text-amber-200" />
                <p className="mt-2 text-xs text-amber-800 dark:text-amber-300">Anyone with this phrase controls your wallet. Never share it.</p>
              </div>
            ) : (
              <p className="mt-1 font-mono text-sm tracking-widest text-zinc-400">•••• •••• •••• •••• •••• ••••</p>
            )}
          </div>
        )}
      </CardBody>
    </Card>
  )
}

function FaucetCard({ wallet }: { wallet: EthWallet }) {
  const faucet = useRequestFaucet()
  const profile = useRegisterProfile()
  const [groupId, setGroupId] = useState('')
  const [result, setResult] = useState<FaucetResult>()
  const enrolled = hasProfile(wallet)
  const balance = Number.parseFloat(wallet.ethAccBalance ?? '0')

  async function requestPayout(e: FormEvent) {
    e.preventDefault()
    try {
      setResult(await faucet.mutateAsync(groupId.trim()))
    } catch (err) {
      toast.error('Pay-out failed', { description: err instanceof Error ? err.message : String(err) })
    }
  }

  async function optIn() {
    try {
      await profile.mutateAsync()
      toast.success('You are now part of the reputation system')
    } catch (err) {
      toast.error('Opt-in failed', { description: err instanceof Error ? err.message : String(err) })
    }
  }

  return (
    <Card>
      <CardHeader
        title="Reputation pay-out"
        description="Earn L2Pcoin for the services you host and develop, weighted by how others rate you."
      />
      <CardBody className="space-y-4">
        <form onSubmit={requestPayout} className="space-y-3">
          <Field label="Success-model group (optional)" hint="Group ID whose MobSOS success model measures your services.">
            <Input value={groupId} onChange={(e) => setGroupId(e.target.value)} placeholder="Group agent ID" />
          </Field>
          <Button type="submit" loading={faucet.isPending} className="w-full">
            <HandCoins className="size-4" /> Request pay-out
          </Button>
        </form>
        {!enrolled && (
          <div className="rounded-lg bg-brand-50 p-4 text-sm dark:bg-brand-500/10">
            <div className="flex items-center gap-2 font-medium">
              <Sparkles className="size-4 text-brand-600" /> Join the reputation system
            </div>
            <p className="mt-1 text-zinc-600 dark:text-zinc-300">
              Opting in creates your public reputation profile on the blockchain, so you can rate others and be rated.
              {balance < 0.01 && ' It costs a little gas: request a pay-out first.'}
            </p>
            <Button className="mt-3" size="sm" onClick={optIn} loading={profile.isPending} disabled={balance < 0.01}>
              <ShieldCheck className="size-4" /> Opt in
            </Button>
          </div>
        )}
      </CardBody>
      <Dialog open={!!result} onClose={() => setResult(undefined)} title="Pay-out sent" footer={<Button onClick={() => setResult(undefined)}>Done</Button>}>
        {result && (
          <div className="space-y-4 text-sm">
            <p className="text-3xl font-semibold">{result.ethFaucetAmount}</p>
            <dl className="grid grid-cols-3 gap-3">
              {(
                [
                  ['User rating', result.rewardDetails.userRatingScore, `×${result.rewardDetails.u}`],
                  ['Hosting', result.rewardDetails.hostingServicesScore, `×${result.rewardDetails.h}`],
                  ['Developing', result.rewardDetails.developServicesScore, `×${result.rewardDetails.d}`],
                ] as const
              ).map(([label, value, weight]) => (
                <div key={label} className="rounded-lg bg-zinc-50 p-3 dark:bg-zinc-800/60">
                  <dt className="text-xs text-zinc-500">{label}</dt>
                  <dd className="font-semibold">
                    {formatNumber(value)} <span className="text-xs font-normal text-zinc-500">{weight}</span>
                  </dd>
                </div>
              ))}
            </dl>
            <p className="text-xs text-zinc-500">Reward = user rating × (hosting + developing), each weighted by the node's faucet settings.</p>
            {[...result.rewardDetails.rewardedForServicesHosting, ...result.rewardDetails.rewardedForServicesDevelop].length > 0 && (
              <div className="flex flex-wrap gap-1">
                {result.rewardDetails.rewardedForServicesHosting.map((s) => (
                  <Badge key={`h${s}`} tone="green">
                    hosted {s}
                  </Badge>
                ))}
                {result.rewardDetails.rewardedForServicesDevelop.map((s) => (
                  <Badge key={`d${s}`} tone="brand">
                    developed {s}
                  </Badge>
                ))}
              </div>
            )}
          </div>
        )}
      </Dialog>
    </Card>
  )
}

function SendDialog({ to, onClose }: { to?: DashboardAgent; onClose: () => void }) {
  const send = useSendCoins()
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!to) return
    const data = new FormData(e.currentTarget)
    try {
      await send.mutateAsync({ agentid: to.agentid, amount: String(data.get('amount')), message: String(data.get('message') ?? '') })
      toast.success(`Sent ${data.get('amount')} L2Pcoin to ${to.username}`)
      onClose()
    } catch {
      // shown in the dialog
    }
  }
  return (
    <Dialog
      open={!!to}
      onClose={onClose}
      title={`Send L2Pcoin to ${to?.username ?? ''}`}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" form="send-coins" loading={send.isPending}>
            Send
          </Button>
        </>
      }
    >
      <form id="send-coins" onSubmit={submit} className="space-y-4">
        <Field label="Amount (L2Pcoin)">
          <Input name="amount" type="number" min="0.0001" step="any" required autoFocus />
        </Field>
        <Field label="Message">
          <Textarea name="message" rows={2} placeholder="Thanks for hosting!" />
        </Field>
        <ErrorNote error={send.error} />
      </form>
    </Dialog>
  )
}

function CommunityCard({ canRate }: { canRate: boolean }) {
  const { agent } = useAuth()
  const dashboard = useDashboard()
  const rate = useRateAgent()
  const [sendTo, setSendTo] = useState<DashboardAgent>()
  const others = (dashboard.data ?? []).filter((a) => a.agentid !== agent?.agentid)

  async function onRate(a: DashboardAgent, rating: number) {
    try {
      await rate.mutateAsync({ agentid: a.agentid, rating })
      toast.success(`Rated ${a.username} ${rating}/5`)
    } catch (err) {
      toast.error(`Could not rate ${a.username}`, { description: err instanceof Error ? err.message : String(err) })
    }
  }

  return (
    <Card>
      <CardHeader title="Community" description={canRate ? 'Rate people you have worked with, or send them L2Pcoin.' : 'Opt in to the reputation system to rate others.'} />
      {dashboard.isPending ? (
        <CardBody>
          <Spinner label="Loading members" />
        </CardBody>
      ) : others.length === 0 ? (
        <EmptyState title="Nobody else here yet">Other users appear once they have a wallet on this network.</EmptyState>
      ) : (
        <Table head={['User', 'Reputation', 'Ratings', 'Your rating', '']}>
          {others.map((a) => (
            <tr key={a.agentid}>
              <Td>
                <div className="font-medium">{a.username}</div>
                <div className="text-xs text-zinc-500">
                  <Copyable value={a.address} display={shortId(a.address, 6, 4)} />
                </div>
              </Td>
              <Td>{a.agentHasProfile ? <Stars value={parseRating(a.ethRating)} /> : <Badge>no profile</Badge>}</Td>
              <Td className="text-xs text-zinc-500 tabular-nums">
                {a.agentHasProfile ? `${a.noOfTransactionsRcvd ?? 0} received · ${a.noOfTransactionsSent ?? 0} given` : '–'}
              </Td>
              <Td>
                {a.agentHasProfile && canRate ? (
                  <Stars value={0} onRate={(n) => void onRate(a, n)} disabled={rate.isPending} label={`Rate ${a.username}`} />
                ) : (
                  <span className="text-xs text-zinc-400">–</span>
                )}
              </Td>
              <Td className="text-right">
                <Button variant="ghost" size="sm" onClick={() => setSendTo(a)} aria-label={`Send L2Pcoin to ${a.username}`}>
                  <Gift className="size-4" />
                </Button>
              </Td>
            </tr>
          ))}
        </Table>
      )}
      <SendDialog to={sendTo} onClose={() => setSendTo(undefined)} />
    </Card>
  )
}

function TxRow({ tx, incoming }: { tx: TxLogEntry; incoming: boolean }) {
  const rating = tx.txTransactionType === USER_RATING_TX
  return (
    <tr>
      <Td className="whitespace-nowrap text-xs text-zinc-500">{tx.txDateTime}</Td>
      <Td>
        <span className="inline-flex items-center gap-1.5">
          {incoming ? <ArrowDownLeft className="size-4 text-emerald-600" /> : <ArrowUpRight className="size-4 text-zinc-400" />}
          <span className="max-w-[16ch] truncate font-mono text-xs" title={incoming ? tx.txSenderAddress : tx.txReceiverAddress}>
            {incoming ? tx.txSenderAddress : tx.txReceiverAddress}
          </span>
        </span>
      </Td>
      <Td>{rating ? <Badge tone="amber">rating</Badge> : <Badge>{tx.txTransactionType || 'transfer'}</Badge>}</Td>
      <Td className="text-sm text-zinc-600 dark:text-zinc-300">{tx.txMessage}</Td>
      <Td className="text-right tabular-nums">{rating ? <Stars value={Number.parseFloat(tx.txAmountInEth)} /> : `${formatNumber(tx.txAmountInEth, 4)} L2P`}</Td>
    </tr>
  )
}

function ActivityCard({ wallet }: { wallet: EthWallet }) {
  const log = useTxLog()
  const [tab, setTab] = useState<'payouts' | 'in' | 'out'>('payouts')
  const tabs = [
    ['payouts', 'Pay-outs', wallet.rcvdTx?.length ?? 0],
    ['in', 'Received', log.data?.rcvdJsonLog?.length ?? 0],
    ['out', 'Sent', log.data?.sentJsonLog?.length ?? 0],
  ] as const
  return (
    <Card>
      <CardHeader title="Activity" />
      <div className="flex gap-1 border-b border-zinc-200 px-3 dark:border-zinc-800" role="tablist">
        {tabs.map(([key, label, count]) => (
          <button
            key={key}
            role="tab"
            aria-selected={tab === key}
            onClick={() => setTab(key)}
            className={cn(
              '-mb-px border-b-2 px-3 py-2 text-sm font-medium',
              tab === key ? 'border-brand-600 text-brand-700 dark:text-brand-100' : 'border-transparent text-zinc-500 hover:text-zinc-900 dark:hover:text-zinc-100',
            )}
          >
            {label} <span className="ml-1 rounded bg-zinc-100 px-1.5 text-xs dark:bg-zinc-800">{count}</span>
          </button>
        ))}
      </div>
      {tab === 'payouts' ? (
        wallet.rcvdTx?.length ? (
          <Table head={['Date', 'Amount']}>
            {wallet.rcvdTx.map((tx, i) => (
              <tr key={i}>
                <Td className="text-xs text-zinc-500">{tx.blockDateTime}</Td>
                <Td className="tabular-nums">{formatNumber(tx.value, 4)} L2P</Td>
              </tr>
            ))}
          </Table>
        ) : (
          <EmptyState icon={<Coins className="size-8" />} title="No pay-outs yet" />
        )
      ) : (
        (() => {
          const rows = (tab === 'in' ? log.data?.rcvdJsonLog : log.data?.sentJsonLog) ?? []
          if (log.isPending) return <CardBody><Spinner /></CardBody>
          if (!rows.length) return <EmptyState title={tab === 'in' ? 'Nothing received yet' : 'Nothing sent yet'} />
          return (
            <Table head={['Date', tab === 'in' ? 'From' : 'To', 'Type', 'Message', 'Value']}>
              {rows.map((tx, i) => (
                <TxRow key={tx.txTXHash ?? i} tx={tx} incoming={tab === 'in'} />
              ))}
            </Table>
          )
        })()
      )}
    </Card>
  )
}

export function WalletPage() {
  const { agent } = useAuth()
  const registry = useHasRegistry()
  const wallet = useWallet()
  const coinbase = useCoinbase()

  const header = (
    <PageHeader
      title="Wallet & reputation"
      description="las2peer rewards people who host and develop community services with L2Pcoin. Reputation comes from ratings by other users and is stored on the blockchain."
    />
  )
  if (registry.data === false)
    return (
      <>
        {header}
        <Card>
          <EmptyState icon={<Wallet className="size-8" />} title="This node has no blockchain registry">
            Wallets and reputation need a node started with the Ethereum registry.
          </EmptyState>
        </Card>
      </>
    )
  if (!agent)
    return (
      <>
        {header}
        <SignInRequired what="see your wallet and reputation" />
      </>
    )

  const w = wallet.data
  return (
    <>
      {header}
      {wallet.isPending && <Spinner label="Loading your wallet" />}
      <ErrorNote error={wallet.error} />
      {w && (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard icon={<Coins className="size-4" />} label="Balance" value={`${formatNumber(w.ethAccBalance, 4)} L2P`} />
            <StatCard
              icon={<ShieldCheck className="size-4" />}
              label="Reputation"
              value={hasProfile(w) ? <Stars value={parseRating(w.ethRating)} /> : <span className="text-base text-zinc-400">not enrolled</span>}
              hint={hasProfile(w) ? `score ${w.ethCumulativeScore}` : 'Opt in below'}
            />
            <StatCard
              icon={<ArrowDownLeft className="size-4" />}
              label="Ratings"
              value={hasProfile(w) ? `${w.ethNoTransactionsRcvd ?? 0} / ${w.ethNoTransactionsSent ?? 0}` : '–'}
              hint="received / given"
            />
            <StatCard
              icon={<HandCoins className="size-4" />}
              label="Reward pool"
              value={coinbase.data ? `${formatNumber(coinbase.data.coinbaseBalance, 0)} L2P` : '–'}
              hint="available for pay-outs on this network"
            />
          </div>
          <div className="grid gap-6 lg:grid-cols-2">
            <WalletCard wallet={w} />
            <FaucetCard wallet={w} />
          </div>
          <CommunityCard canRate={hasProfile(w)} />
          <ActivityCard wallet={w} />
        </div>
      )}
    </>
  )
}
