import { useState } from 'react'
import { Star } from 'lucide-react'
import { cn } from '../lib/cn'

/** Read-only rating out of 5, or an input when onRate is set. */
export function Stars({ value, onRate, disabled, label = 'Rating' }: { value: number; onRate?: (n: number) => void; disabled?: boolean; label?: string }) {
  const [hover, setHover] = useState<number>()
  const shown = hover ?? value
  if (!onRate)
    return (
      <span className="inline-flex items-center gap-0.5" role="img" aria-label={`${label}: ${value.toFixed(1)} of 5`}>
        {[1, 2, 3, 4, 5].map((i) => (
          <Star key={i} className={cn('size-3.5', i <= Math.round(shown) ? 'fill-amber-400 text-amber-400' : 'text-zinc-300 dark:text-zinc-600')} />
        ))}
      </span>
    )
  return (
    <span className="inline-flex items-center" role="radiogroup" aria-label={label} onMouseLeave={() => setHover(undefined)}>
      {[1, 2, 3, 4, 5].map((i) => (
        <button
          key={i}
          type="button"
          role="radio"
          aria-checked={Math.round(value) === i}
          aria-label={`${i} star${i > 1 ? 's' : ''}`}
          disabled={disabled}
          onMouseEnter={() => setHover(i)}
          onClick={() => onRate(i)}
          className="rounded p-0.5 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <Star className={cn('size-4 transition-colors', i <= Math.round(shown) ? 'fill-amber-400 text-amber-400' : 'text-zinc-300 dark:text-zinc-600')} />
        </button>
      ))}
    </span>
  )
}
