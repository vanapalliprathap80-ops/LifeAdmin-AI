import type { DocumentType, KeyDateType, BillingFrequency, ActionDto, ServiceDto, TimelineItem } from './types';

/** Format an ISO date string to a readable format like "Oct 14, 2026" */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  try {
    const d = new Date(iso + (iso.includes('T') ? '' : 'T00:00:00'));
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  } catch {
    return iso;
  }
}

/** Format an ISO instant to a readable format like "Sep 18, 2026 at 12:00 PM" */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  try {
    const d = new Date(iso);
    return d.toLocaleDateString('en-US', {
      month: 'short', day: 'numeric', year: 'numeric',
      hour: 'numeric', minute: '2-digit',
    });
  } catch {
    return iso;
  }
}

/** Calculate days remaining from today to an ISO date string */
export function daysRemaining(iso: string | null | undefined): number | null {
  if (!iso) return null;
  try {
    const d = new Date(iso + (iso.includes('T') ? '' : 'T00:00:00'));
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    return Math.round((d.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
  } catch {
    return null;
  }
}

/** Format relative time like "3 days ago" or "in 14 days" */
export function formatRelative(iso: string | null | undefined): string {
  const days = daysRemaining(iso);
  if (days === null) return '';
  if (days === 0) return 'today';
  if (days === 1) return 'tomorrow';
  if (days === -1) return 'yesterday';
  if (days > 0) return `in ${days} days`;
  return `${Math.abs(days)} days ago`;
}

/** Urgency color class based on days remaining */
export function urgencyColor(days: number | null): string {
  if (days === null) return 'var(--text-secondary)';
  if (days <= 3) return 'var(--critical)';
  if (days <= 7) return 'var(--warning)';
  if (days <= 30) return 'var(--primary-hover)';
  return 'var(--success)';
}

/** Urgency dot for Action Center */
export function urgencyDot(days: number | null): string {
  if (days === null) return '○';
  if (days <= 3) return '🔴';
  if (days <= 7) return '🟠';
  if (days <= 30) return '🟡';
  return '🟢';
}

/** Format file size to readable string */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** Human-readable document type */
export function formatDocType(t: DocumentType): string {
  const map: Record<DocumentType, string> = {
    INSURANCE: 'Insurance',
    LEASE: 'Lease',
    BILL: 'Bill',
    WARRANTY: 'Warranty',
    SUBSCRIPTION: 'Subscription',
    UNKNOWN: 'Document',
  };
  return map[t] ?? t;
}

/** Human-readable key date type */
export function formatKeyDateType(t: KeyDateType | string): string {
  return t.replace(/_/g, ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase());
}

/** Human-readable billing frequency */
export function formatBillingFrequency(f: BillingFrequency | string | null): string {
  if (!f) return '';
  const map: Record<string, string> = {
    MONTHLY: '/month',
    QUARTERLY: '/quarter',
    HALF_YEARLY: '/6 months',
    YEARLY: '/year',
    ONE_TIME: ' (one-time)',
  };
  return map[f] ?? '';
}

/** Document type icon emoji */
export function docTypeIcon(t: DocumentType): string {
  const map: Record<DocumentType, string> = {
    INSURANCE: '🛡️',
    LEASE: '🏠',
    BILL: '💳',
    WARRANTY: '🔧',
    SUBSCRIPTION: '📋',
    UNKNOWN: '📄',
  };
  return map[t] ?? '📄';
}

/** Format price with currency symbol */
export function formatPrice(price: number | null, frequency?: BillingFrequency | string | null): string {
  if (price === null || price === undefined) return '';
  const formatted = new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR', minimumFractionDigits: 0, maximumFractionDigits: 0,
  }).format(price);
  return formatted + (frequency ? formatBillingFrequency(frequency) : '');
}

/**
 * Build unified timeline items from actions + services for the Action Center.
 * Services with renewal dates become timeline items alongside document actions.
 */
export function buildTimeline(actions: ActionDto[], services: ServiceDto[]): TimelineItem[] {
  const items: TimelineItem[] = [];

  // Document-based actions
  for (const a of actions) {
    const days = daysRemaining(a.deadline ?? a.recommendedDate);
    items.push({
      id: `action-${a.id}`,
      source: 'document',
      title: a.title,
      subtitle: a.description ?? '',
      deadline: a.deadline ?? a.recommendedDate,
      daysRemaining: days,
      priority: a.priority,
      status: a.status,
      reason: a.reason,
      evidence: a.evidence,
      documentId: a.documentId,
      serviceId: null,
      officialUrl: null,
      action: a,
    });
  }

  // Service renewals as timeline items (only if they have a renewal date)
  for (const s of services) {
    if (!s.renewalDate) continue;
    const days = daysRemaining(s.renewalDate);
    const priority = days !== null && days <= 3 ? 'CRITICAL'
      : days !== null && days <= 7 ? 'HIGH'
      : days !== null && days <= 30 ? 'MEDIUM'
      : 'LOW' as const;
    items.push({
      id: `service-${s.id}`,
      source: 'service',
      title: `${s.name} renewal`,
      subtitle: s.currentPlan
        ? `${s.currentPlan}${s.price ? ' — ' + formatPrice(s.price, s.billingFrequency) : ''}`
        : s.price ? formatPrice(s.price, s.billingFrequency) : 'Renewal approaching',
      deadline: s.renewalDate,
      daysRemaining: days,
      priority,
      status: 'PENDING',
      reason: `Your ${s.name} renewal date is ${formatDate(s.renewalDate)}.${s.currentPlan ? `\n\nCurrent plan: ${s.currentPlan}` : ''}${s.price ? `\nPrice: ${formatPrice(s.price, s.billingFrequency)}` : ''}\n\nSource: User-provided service information`,
      evidence: null,
      documentId: null,
      serviceId: s.id,
      officialUrl: s.officialUrl,
      service: s,
    });
  }

  return items;
}

/** Sort timeline: critical first, then by deadline (soonest first), nulls last */
export function sortTimeline(items: TimelineItem[]): TimelineItem[] {
  const priorityOrder = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
  return [...items].sort((a, b) => {
    // Completed items last
    if (a.status === 'COMPLETED' && b.status !== 'COMPLETED') return 1;
    if (b.status === 'COMPLETED' && a.status !== 'COMPLETED') return -1;
    // Priority
    const pa = priorityOrder[a.priority] ?? 9;
    const pb = priorityOrder[b.priority] ?? 9;
    if (pa !== pb) return pa - pb;
    // Deadline soonest first
    const da = a.daysRemaining ?? 999;
    const db = b.daysRemaining ?? 999;
    return da - db;
  });
}
