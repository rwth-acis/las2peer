export function shortId(id: string | undefined, head = 8, tail = 6): string {
  if (!id) return ''
  return id.length <= head + tail + 1 ? id : `${id.slice(0, head)}…${id.slice(-tail)}`
}

export function formatDate(value: string | number | Date | undefined): string {
  if (value === undefined || value === '') return ''
  const date = typeof value === 'number' ? new Date(value < 1e12 ? value * 1000 : value) : new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
}

export function formatNumber(value: number | string | undefined, digits = 2): string {
  const n = typeof value === 'string' ? Number.parseFloat(value) : value
  if (n === undefined || Number.isNaN(n)) return String(value ?? '')
  return n.toLocaleString(undefined, { maximumFractionDigits: digits })
}
