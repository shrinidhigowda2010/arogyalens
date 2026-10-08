import type { ParameterStatus } from '../types'

export function statusLabel(status: ParameterStatus): string {
  switch (status) {
    case 'WITHIN_RANGE':
      return 'Within range'
    case 'OUTSIDE_RANGE':
      return 'Outside range on report'
    case 'REQUIRES_DISCUSSION':
      return 'Needs discussion'
    case 'IMPORTANT_ATTENTION':
      return 'Important attention'
    case 'LOW_CONFIDENCE':
      return 'Unclear reading'
    default:
      return 'Review with clinician'
  }
}

export function statusTone(status: ParameterStatus): string {
  switch (status) {
    case 'WITHIN_RANGE':
      return 'bg-emerald-50 text-emerald-900 border-emerald-200'
    case 'REQUIRES_DISCUSSION':
    case 'OUTSIDE_RANGE':
      return 'bg-amber-50 text-amber-950 border-amber-200'
    case 'IMPORTANT_ATTENTION':
      return 'bg-red-50 text-red-950 border-red-200'
    case 'LOW_CONFIDENCE':
      return 'bg-slate-100 text-slate-800 border-slate-200'
    default:
      return 'bg-slate-50 text-slate-800 border-slate-200'
  }
}

export function dashboardBucketLabel(key: 'within' | 'discuss' | 'attention'): string {
  switch (key) {
    case 'within':
      return 'Within range'
    case 'discuss':
      return 'Needs discussion'
    case 'attention':
      return 'Important attention'
  }
}
