import React, { useEffect, useState } from 'react';
import { getNotifications } from '../api/notificationsApi';
import type { NotificationDto } from '../types';

const CATEGORY_LABELS: Record<string, { label: string; icon: string; color: string }> = {
  DEADLINE_REMINDER_30_DAYS: { label: '30 Days Until Deadline', icon: '📅', color: 'var(--primary)' },
  DEADLINE_REMINDER_7_DAYS: { label: '7 Days Until Deadline', icon: '⚠️', color: 'var(--warning)' },
  DEADLINE_REMINDER_1_DAY: { label: '1 Day Until Deadline', icon: '🔥', color: 'var(--critical)' },
  DEADLINE_OVERDUE: { label: 'Deadline Overdue', icon: '🚨', color: 'var(--critical)' },
  OBLIGATION_DETECTED: { label: 'New Obligation Detected', icon: '📋', color: 'var(--accent)' },
};

export const NotificationsPage: React.FC = () => {
  const [notifications, setNotifications] = useState<NotificationDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState<string>('ALL');

  useEffect(() => {
    getNotifications()
      .then(setNotifications)
      .catch(err => setError(err instanceof Error ? err.message : 'Failed to load notifications.'))
      .finally(() => setLoading(false));
  }, []);

  const filtered = filter === 'ALL' 
    ? notifications 
    : notifications.filter(n => n.category === filter);

  const categories = ['ALL', ...Object.keys(CATEGORY_LABELS)];

  return (
    <>
      <div className="page-header">
        <h1>Notifications</h1>
        <p>Track all deadline reminders, obligation alerts, and system updates.</p>
      </div>

      {error && <div className="error-banner">⚠ {error}</div>}

      {/* Filter tabs */}
      <div style={{ display: 'flex', gap: 'var(--space-xs)', flexWrap: 'wrap', marginBottom: 'var(--space-xl)' }}>
        {categories.map(cat => (
          <button
            key={cat}
            className={`btn ${filter === cat ? 'btn-primary' : 'btn-ghost'}`}
            style={{ padding: '6px 14px', fontSize: '0.8rem' }}
            onClick={() => setFilter(cat)}
          >
            {cat === 'ALL' ? 'All' : (CATEGORY_LABELS[cat]?.label || cat.replace(/_/g, ' '))}
          </button>
        ))}
      </div>

      {loading ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-md)' }}>
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="skeleton" style={{ height: 80, borderRadius: 'var(--radius-lg)' }} />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🔔</div>
          <h3>{filter === 'ALL' ? 'No notifications yet' : 'No notifications in this category'}</h3>
          <p>When deadlines approach or new obligations are detected, you'll see them here.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-sm)' }}>
          {filtered.map(notif => {
            const catInfo = CATEGORY_LABELS[notif.category] || { label: notif.category, icon: '🔔', color: 'var(--text-secondary)' };
            const lines = notif.messageContent.split('\n');
            const subject = lines[0] || '';
            const bodyPreview = lines.slice(1).join(' ').trim();

            return (
              <div
                key={notif.id}
                className="card"
                style={{
                  display: 'flex',
                  gap: 'var(--space-md)',
                  alignItems: 'flex-start',
                  borderLeft: `3px solid ${catInfo.color}`,
                  transition: 'all 0.2s ease',
                }}
              >
                <div style={{ fontSize: '1.5rem', flexShrink: 0, marginTop: '2px' }}>
                  {catInfo.icon}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                    <span style={{
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      textTransform: 'uppercase',
                      letterSpacing: '0.05em',
                      color: catInfo.color,
                    }}>
                      {catInfo.label}
                    </span>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', flexShrink: 0 }}>
                      {new Date(notif.sentAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                  <div style={{ fontWeight: 600, marginBottom: '4px', color: 'var(--text-primary)' }}>
                    {subject}
                  </div>
                  {bodyPreview && (
                    <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                      {bodyPreview.length > 200 ? bodyPreview.substring(0, 200) + '...' : bodyPreview}
                    </div>
                  )}
                  <div style={{ marginTop: '6px', display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <span className="badge" style={{
                      fontSize: '0.7rem',
                      background: notif.type === 'EMAIL' ? 'var(--primary-subtle)' : 'var(--accent-subtle)',
                      color: notif.type === 'EMAIL' ? 'var(--primary)' : 'var(--accent)',
                    }}>
                      {notif.type === 'EMAIL' ? '📧 Email' : '🔔 In-App'}
                    </span>
                    <span className="badge" style={{
                      fontSize: '0.7rem',
                      background: notif.status === 'SENT' ? 'var(--success-subtle)' : 'var(--warning-subtle)',
                      color: notif.status === 'SENT' ? 'var(--success)' : 'var(--warning)',
                    }}>
                      {notif.status}
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </>
  );
};
