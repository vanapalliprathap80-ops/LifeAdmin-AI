import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { listActions } from '../api/actionsApi';
import { listServices } from '../api/servicesApi';
import { getNotifications } from '../api/notificationsApi';
import type { ActionDto, ServiceDto, NotificationDto } from '../types';
import { TimelineCard } from '../components/TimelineCard';
import { UploadZone } from '../components/UploadZone';
import { buildTimeline, sortTimeline } from '../utils';

export const ActionCenterPage: React.FC = () => {
  const [actions, setActions] = useState<ActionDto[]>([]);
  const [services, setServices] = useState<ServiceDto[]>([]);
  const [notifications, setNotifications] = useState<NotificationDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [acts, srvs, notifs] = await Promise.all([
        listActions(),
        listServices().catch(() => []), // Don't fail entire page if services fail
        getNotifications().catch(() => []) // Optional notifications
      ]);
      setActions(acts);
      setServices(srvs);
      setNotifications(notifs);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load Action Center data.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const timeline = sortTimeline(buildTimeline(actions, services));
  
  const pendingItems = timeline.filter(t => t.status !== 'COMPLETED');
  const completedItems = timeline.filter(t => t.status === 'COMPLETED');
  
  const needsAttention = pendingItems.filter(t => t.priority === 'CRITICAL' || t.priority === 'HIGH');
  const upcoming = pendingItems.filter(t => t.priority !== 'CRITICAL' && t.priority !== 'HIGH');

  const greeting = () => {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning';
    if (h < 18) return 'Good afternoon';
    return 'Good evening';
  };

  return (
    <>
      <div className="hero">
        <div className="hero-orb hero-orb-1" />
        <div className="hero-orb hero-orb-2" />
        <h1>
          <span className="gradient-text">Turn your documents</span><br />
          into actions.
        </h1>
        <p className="subtitle">
          Understand what your documents require, know when you need to act, and get to the right next step.
        </p>
        
        <div className="hero-ctas">
          <UploadZone onUploadComplete={() => {
            fetchData();
            navigate('/documents');
          }} />
        </div>
      </div>

      <div style={{ marginTop: 'var(--space-3xl)' }}>
        {notifications.length > 0 && (
          <div style={{ marginBottom: 'var(--space-2xl)' }}>
            <h2>Recent Notifications</h2>
            <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-sm)' }}>
              {notifications.slice(0, 3).map(notif => (
                <div key={notif.id} style={{ display: 'flex', gap: 'var(--space-md)', alignItems: 'center', padding: 'var(--space-sm) 0', borderBottom: '1px solid var(--border-color)' }}>
                  <div style={{ fontSize: '1.5rem' }}>🔔</div>
                  <div>
                    <div style={{ fontWeight: 600 }}>{notif.category.replace(/_/g, ' ')}</div>
                    <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>{notif.messageContent.split('\n')[0]}</div>
                  </div>
                  <div style={{ marginLeft: 'auto', fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>
                    {new Date(notif.sentAt).toLocaleDateString()}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="page-header">
          <h1>{greeting()}</h1>
          <p>
            {pendingItems.length > 0
              ? `Here's what needs your attention.`
              : 'Your life admin is under control.'
            }
          </p>
        </div>

        {error && <div className="error-banner">⚠ {error}</div>}

        {!error && (
          <>
            {loading ? (
              <div className="doc-grid">
                {[1, 2, 3].map(i => (
                  <div key={i} className="skeleton" style={{ height: 200, borderRadius: 'var(--radius-lg)' }} />
                ))}
              </div>
            ) : timeline.length === 0 ? (
              <div className="empty-state">
                <div className="empty-state-icon">📋</div>
                <h3>Nothing needs your attention.</h3>
                <p>Upload a document or add a service to get started.</p>
                <div style={{ marginTop: 'var(--space-lg)', display: 'flex', gap: '1rem', justifyContent: 'center' }}>
                  <button className="btn btn-primary" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
                    Upload Document
                  </button>
                  <button className="btn btn-outline-primary" onClick={() => navigate('/services')}>
                    Add Service
                  </button>
                </div>
              </div>
            ) : (
              <div>
                {needsAttention.length > 0 && (
                  <div className="urgency-section">
                    <div className="urgency-section-header">
                      <h2>Needs Attention</h2>
                      <span className="urgency-section-count">{needsAttention.length}</span>
                    </div>
                    <div>
                      {needsAttention.map(item => (
                        <TimelineCard key={item.id} item={item} onActionCompleted={fetchData} />
                      ))}
                    </div>
                  </div>
                )}

                {upcoming.length > 0 && (
                  <div className="urgency-section">
                    <div className="urgency-section-header">
                      <h2>Upcoming</h2>
                      <span className="urgency-section-count">{upcoming.length}</span>
                    </div>
                    <div>
                      {upcoming.map(item => (
                        <TimelineCard key={item.id} item={item} onActionCompleted={fetchData} />
                      ))}
                    </div>
                  </div>
                )}

                {completedItems.length > 0 && (
                  <div className="urgency-section">
                    <div className="urgency-section-header">
                      <h2>Recently Completed</h2>
                      <span className="urgency-section-count">{completedItems.length}</span>
                    </div>
                    <div>
                      {completedItems.map(item => (
                        <TimelineCard key={item.id} item={item} />
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </>
  );
};
