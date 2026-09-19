/* ════════════════════════════════════════════════════════════════
   LifeAdmin AI — TypeScript Types
   Source of truth: backend DTOs
   ════════════════════════════════════════════════════════════════ */

// ── Enums (matching backend exactly) ───────────────────────────

export type DocumentType = 'INSURANCE' | 'LEASE' | 'BILL' | 'WARRANTY' | 'SUBSCRIPTION' | 'UNKNOWN';

export type ProcessingStatus = 'UPLOADED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'NEEDS_REVIEW';

export type ActionPriority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';

export type ActionStatus = 'PENDING' | 'COMPLETED' | 'NEEDS_REVIEW';

export type KeyDateType =
  | 'START_DATE' | 'END_DATE' | 'RENEWAL_DATE' | 'PAYMENT_DUE'
  | 'EXPIRY_DATE' | 'EFFECTIVE_DATE' | 'REVIEW_DATE'
  | 'CANCELLATION_DEADLINE' | 'NOTICE_PERIOD_END' | 'OTHER';

export type ObligationStatus = 'IDENTIFIED' | 'CONFIRMED' | 'DISMISSED';

export type BillingFrequency = 'MONTHLY' | 'QUARTERLY' | 'HALF_YEARLY' | 'YEARLY' | 'ONE_TIME';

// ── API Response Envelope ──────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error: string | null;
  timestamp: string;
}

// ── Document DTOs ──────────────────────────────────────────────

export interface DocumentDto {
  id: string;
  originalFilename: string;
  documentType: DocumentType;
  processingStatus: ProcessingStatus;
  fileSize: number;
  contentType: string;
  summary: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface KeyDateDto {
  id: string;
  dateValue: string;
  dateType: KeyDateType;
  description: string | null;
  evidence: string | null;
  confidence: number | null;
}

export interface ObligationDto {
  id: string;
  obligationType: string;
  description: string;
  evidence: string | null;
  confidence: number | null;
  status: ObligationStatus;
  serviceId?: string | null;
}

export interface DocumentDetailDto {
  id: string;
  originalFilename: string;
  documentType: DocumentType;
  processingStatus: ProcessingStatus;
  fileSize: number;
  contentType: string;
  summary: string | null;
  keyDates: KeyDateDto[];
  obligations: ObligationDto[];
  actions: ActionDto[];
  createdAt: string;
  updatedAt: string;
}

// ── Action DTOs ────────────────────────────────────────────────

export interface ActionDto {
  id: string;
  documentId: string | null;
  serviceId?: string | null;
  obligationId: string | null;
  title: string;
  description: string | null;
  deadline: string | null;
  recommendedDate: string | null;
  priority: ActionPriority;
  status: ActionStatus;
  reason: string | null;
  evidence: string | null;
  createdAt: string;
  completedAt: string | null;
}

// ── Service / Subscription DTOs ────────────────────────────────

export interface ServiceDto {
  id: string;
  name: string;
  currentPlan: string | null;
  price: number | null;
  billingFrequency: BillingFrequency | null;
  renewalDate: string | null;
  officialUrl: string | null;
  notes: string | null;
  serviceType: 'SUBSCRIPTION' | 'WARRANTY' | 'PRODUCT';
  assetName: string | null;
  providerConnectionId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateServiceRequest {
  name: string;
  currentPlan?: string;
  price?: number;
  billingFrequency?: string;
  renewalDate?: string;
  officialUrl?: string;
  notes?: string;
  serviceType?: string;
  assetName?: string;
}

// ── Unified timeline item for Action Center ────────────────────

export type TimelineItemSource = 'document' | 'service';

export interface TimelineItem {
  id: string;
  source: TimelineItemSource;
  title: string;
  subtitle: string;
  deadline: string | null;
  daysRemaining: number | null;
  priority: ActionPriority;
  status: ActionStatus;
  reason: string | null;
  evidence: string | null;
  documentId: string | null;
  serviceId: string | null;
  officialUrl: string | null;
  action?: ActionDto;
  service?: ServiceDto;
}

// ── Integration / Connections DTOs ──────────────────────────────

export type ConnectionStatus = 'CONNECTED' | 'DISCONNECTED' | 'CONNECTING' | 'SYNCING' | 'SYNCED' | 'CONNECTION_ERROR' | 'EXPIRED';

export interface ProviderConnectionDto {
  id: string;
  providerName: string;
  providerAccountId: string | null;
  status: ConnectionStatus;
  lastSyncedAt: string | null;
  createdAt: string;
}

export interface ConnectProviderRequest {
  providerName: string;
  authCode?: string;
  credentials?: Record<string, string>;
}

export interface ProviderCatalogDto {
  id: string;
  displayName: string;
  category: string;
  description: string;
  officialPortalUrl: string;
}

// ── Notifications DTOs ────────────────────────────────────────

export type NotificationType = 'EMAIL' | 'IN_APP';
export type NotificationCategory = 'DEADLINE_REMINDER_30_DAYS' | 'DEADLINE_REMINDER_7_DAYS' | 'DEADLINE_REMINDER_1_DAY' | 'DEADLINE_OVERDUE' | 'OBLIGATION_DETECTED';
export type NotificationStatus = 'SENT' | 'FAILED' | 'DELIVERED' | 'READ';

export interface NotificationDto {
  id: string;
  actionId: string | null;
  type: NotificationType;
  category: NotificationCategory;
  status: NotificationStatus;
  messageContent: string;
  sentAt: string;
}

