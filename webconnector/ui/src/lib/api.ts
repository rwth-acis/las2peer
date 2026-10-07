// Thin client for the web connector's REST API (same origin, session cookie).

export const API_BASE = '/las2peer'

export class ApiError extends Error {
  readonly status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

type Body = FormData | URLSearchParams | Record<string, unknown> | undefined

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: Body
  query?: Record<string, string | number | undefined>
  headers?: Record<string, string>
}

/** Turns the connector's error payloads ({code,msg,stacktrace}, plain text) into a readable message. */
async function errorMessage(res: Response): Promise<string> {
  const text = await res.text()
  try {
    const json = JSON.parse(text)
    return json.msg || json.message || json.text || text
  } catch {
    return text || res.statusText
  }
}

export async function request<T = unknown>(path: string, opts: RequestOptions = {}): Promise<T> {
  const url = new URL(API_BASE + path, window.location.origin)
  for (const [k, v] of Object.entries(opts.query ?? {})) if (v !== undefined) url.searchParams.set(k, String(v))

  let body: BodyInit | undefined
  const headers: Record<string, string> = { ...opts.headers }
  if (opts.body instanceof FormData || opts.body instanceof URLSearchParams) body = opts.body
  else if (opts.body) {
    body = JSON.stringify(opts.body)
    headers['Content-Type'] = 'application/json'
  }

  const res = await fetch(url, { method: opts.method ?? (body ? 'POST' : 'GET'), body, headers, credentials: 'include' })
  if (!res.ok) throw new ApiError(res.status, await errorMessage(res))
  const text = await res.text()
  if (!text) return undefined as T
  try {
    return JSON.parse(text) as T
  } catch {
    return text as T
  }
}

/** Builds multipart form data, skipping undefined values. */
export function form(fields: Record<string, string | Blob | undefined>): FormData {
  const fd = new FormData()
  for (const [k, v] of Object.entries(fields)) if (v !== undefined) fd.append(k, v)
  return fd
}
