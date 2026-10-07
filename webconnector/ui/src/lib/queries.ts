import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, form, request } from './api'
import { useAuth } from './auth'

// ── types (field names as returned by the web connector) ────────────────────

export interface NodeStatus {
  nodeId: string
  cpuLoad: number
  ramLoad: number
  maxRamLoad: number
  ramLoadStr: string
  maxRamLoadStr: string
  storageSize: number
  maxStorageSize: number
  storageSizeStr: string
  maxStorageSizeStr: string
  uptime: string
  nodeAdminName?: string
  nodeAdminEmail?: string
  nodeOrganization?: string
  nodeDescription?: string
  nodeAdminReputation?: number
  localServices?: { name: string; version: string; swagger?: string }[]
}

export interface NodeInfo {
  description?: string
  'admin-name'?: string
  'admin-mail'?: string
  organization?: string
  'service-count'?: number
  services?: { 'service-name': string; 'service-version': string }[]
}

export interface OtherNode {
  nodeID: string
  nodeInfo: NodeInfo
  nodeAdminReputation?: number
}

export interface ServiceInstance {
  className: string
  nodeId: string
  hosterReputation: number
  announcementEpochSeconds: number
  nodeInfo: NodeInfo
}

export interface ServiceRelease {
  publicationEpochSeconds: number
  supplement: { class?: string; name?: string; description?: string; vcsUrl?: string; frontendUrl?: string }
  instances: ServiceInstance[]
}

export interface RegistryService {
  name: string
  authorName: string
  authorReputation: number
  releases: Record<string, ServiceRelease>
}

export interface AgentInfo {
  agentid: string
  username?: string
  email?: string
}

export interface EthWallet {
  agentid: string
  username?: string
  email?: string
  ethAgentCredentialsAddress?: string
  ethMnemonic?: string
  ethAccBalance?: string
  ethRating?: number | string
  ethCumulativeScore?: number | string
  ethNoTransactionsSent?: number | string
  ethNoTransactionsRcvd?: number | string
  agentHasProfile?: number | boolean
  rcvdTx?: { blockDateTime: string; value: string; from?: string; to?: string }[]
}

export interface DashboardAgent {
  agentid: string
  username: string
  address: string
  agentHasProfile: boolean
  ethRating?: number | string
  noOfTransactionsRcvd?: number
  noOfTransactionsSent?: number
}

export interface TxLogEntry {
  txDateTime: string
  txSenderAddress: string
  txReceiverAddress: string
  txTransactionType: string
  txMessage: string
  txAmountInEth: string
  txTXHash?: string
}

export interface FaucetResult {
  ethFaucetAmount: string
  txHash?: string
  rewardDetails: {
    userRatingScore: number
    hostingServicesScore: number
    developServicesScore: number
    rewardedForServicesHosting: string[]
    rewardedForServicesDevelop: string[]
    u: number
    h: number
    d: number
  }
}

/** ethRating comes as a locale-formatted string ("4.50" or "4,50") or a number. */
export function parseRating(value: number | string | undefined): number {
  if (value === undefined) return 0
  const n = typeof value === 'number' ? value : Number.parseFloat(value.replace(',', '.'))
  return Number.isFinite(n) ? n : 0
}

export const USER_RATING_TX = 'L2P USER RATING'

// ── node ────────────────────────────────────────────────────────────────────

export const useNodeVersion = () =>
  useQuery({ queryKey: ['version'], queryFn: () => request<string>('/version'), staleTime: Infinity })

export const useNodeStatus = () =>
  useQuery({ queryKey: ['status'], queryFn: () => request<NodeStatus>('/status'), refetchInterval: 3000 })

export const useOtherNodes = () =>
  useQuery({ queryKey: ['other-nodes'], queryFn: () => request<OtherNode[]>('/getOtherNodesInfo'), refetchInterval: 30_000 })

export const useHasRegistry = () =>
  useQuery({
    queryKey: ['check-eth'],
    queryFn: () =>
      request('/check-eth')
        .then(() => true)
        .catch(() => false),
    staleTime: Infinity,
  })

// ── services ────────────────────────────────────────────────────────────────

export const useLocalNodeId = () =>
  useQuery({
    queryKey: ['node-id'],
    queryFn: () => request<{ id: string }>('/services/node-id', { headers: { Accept: 'application/json' } }).then((r) => r.id),
    staleTime: Infinity,
  })

export const useRegistryServices = () =>
  useQuery({
    queryKey: ['services'],
    queryFn: () => request<RegistryService[]>('/services/services'),
    refetchInterval: 15_000,
    retry: (count, error) => !(error instanceof ApiError && error.status === 404) && count < 1,
  })

export function useServiceControl() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ action, serviceName, version }: { action: 'start' | 'stop'; serviceName: string; version: string }) =>
      request(`/services/${action}`, { method: 'POST', query: { serviceName, version } }),
    onSettled: () => {
      void qc.invalidateQueries({ queryKey: ['services'] })
      void qc.invalidateQueries({ queryKey: ['status'] })
    },
  })
}

export function usePublishService() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ jar, supplement }: { jar: File; supplement: Record<string, string> }) =>
      request<{ msg: string }>('/services/upload', { body: form({ jarfile: jar, supplement: JSON.stringify(supplement) }) }),
    onSuccess: () => void qc.invalidateQueries({ queryKey: ['services'] }),
  })
}

// ── agents & groups ─────────────────────────────────────────────────────────

export const findAgent = (by: { agentid?: string; username?: string; email?: string }) =>
  request<AgentInfo>('/agents/getAgent', { body: form(by) })

export const createAgent = (fields: { username: string; email?: string; password: string }) =>
  request<AgentInfo & { registryAddress?: string }>('/agents/createAgent', { body: form(fields) })

export async function exportAgent(by: { agentid?: string; username?: string; email?: string }): Promise<Blob> {
  const res = await fetch('/las2peer/agents/exportAgent', { method: 'POST', body: form(by), credentials: 'include' })
  if (!res.ok) throw new ApiError(res.status, res.status === 404 ? 'Agent not found' : await res.text())
  return res.blob()
}

export const uploadAgent = (file: File, password?: string) =>
  request<{ agentid: string; text: string }>('/agents/uploadAgent', { body: form({ agentFile: file, password: password || undefined }) })

export const createGroup = (name: string, members: AgentInfo[]) =>
  request<{ agentid: string; groupName: string }>('/agents/createGroup', {
    body: form({ name, members: JSON.stringify(members) }),
  })

export const loadGroup = (groupIdentifier: string) =>
  request<{ agentid: string; members: AgentInfo[] }>('/agents/loadGroup', { body: form({ groupIdentifier }) })

export const changeGroup = (agentid: string, members: AgentInfo[]) =>
  request('/agents/changeGroup', { body: form({ agentid, members: JSON.stringify(members) }) })

// ── wallet & reputation (Ethereum registry; all need a session) ─────────────

export function useWallet() {
  const { agent } = useAuth()
  return useQuery({
    queryKey: ['wallet', agent?.agentid],
    queryFn: () => request<EthWallet>('/eth/getEthWallet', { method: 'POST' }),
    enabled: !!agent,
    refetchInterval: 10_000,
  })
}

export function useTxLog() {
  const { agent } = useAuth()
  return useQuery({
    queryKey: ['txlog', agent?.agentid],
    queryFn: () => request<{ rcvdJsonLog: TxLogEntry[]; sentJsonLog: TxLogEntry[] }>('/eth/getGenericTxLog', { method: 'POST' }),
    enabled: !!agent,
    refetchInterval: 15_000,
  })
}

export function useDashboard() {
  const { agent } = useAuth()
  return useQuery({
    queryKey: ['dashboard', agent?.agentid],
    queryFn: () => request<{ agentList: DashboardAgent[] }>('/eth/dashboardList', { method: 'POST' }).then((r) => r.agentList ?? []),
    enabled: !!agent,
    refetchInterval: 15_000,
  })
}

export const useCoinbase = () =>
  useQuery({
    queryKey: ['coinbase'],
    queryFn: () => request<{ coinbaseBalance: string; coinbaseAddress: string }>('/eth/getCoinbaseBalance', { method: 'POST' }),
    refetchInterval: 15_000,
  })

function useWalletMutation<TArgs, TResult>(fn: (args: TArgs) => Promise<TResult>) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSettled: () => {
      for (const key of ['wallet', 'txlog', 'dashboard', 'coinbase']) void qc.invalidateQueries({ queryKey: [key] })
    },
  })
}

export const useRequestFaucet = () =>
  useWalletMutation((groupID: string) => request<FaucetResult>('/eth/requestFaucet', { body: form({ groupID }) }))

export const useRegisterProfile = () =>
  useWalletMutation(() => request<{ callTransactionHash: string }>('/eth/registerProfile', { method: 'POST' }))

export const useRateAgent = () =>
  useWalletMutation(({ agentid, rating }: { agentid: string; rating: number }) =>
    request<{ recipientname: string; rating: number }>('/eth/rateAgent', { body: form({ agentid, rating: String(rating) }) }),
  )

export const useSendCoins = () =>
  useWalletMutation(({ agentid, amount, message }: { agentid: string; amount: string; message: string }) =>
    // the field is called weiAmount but the node expects L2Pcoin (ether) and converts it
    request('/eth/addTransaction', { body: form({ agentid, weiAmount: amount, message }) }),
  )
