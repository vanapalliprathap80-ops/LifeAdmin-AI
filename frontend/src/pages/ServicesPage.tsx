import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { listServices } from '../api/servicesApi';
import type { ServiceDto } from '../types';
import { ServiceCard } from '../components/ServiceCard';

export const ServicesPage: React.FC = () => {
  const [services, setServices] = useState<ServiceDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    listServices()
      .then(setServices)
      .catch(err => setError(err instanceof Error ? err.message : 'Failed to load services.'))
      .finally(() => setLoading(false));
  }, []);

  const manualServices = services.filter(s => !s.providerConnectionId);
  const connectedServices = services.filter(s => s.providerConnectionId);

  return (
    <>
      <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1>Services & Assets</h1>
          <p>Manage your subscriptions, warranties, and connected services.</p>
        </div>
        <div style={{ display: 'flex', gap: 'var(--space-sm)' }}>
          <Link to="/services/new" className="btn btn-primary">
            + Add Service
          </Link>
          <Link to="/integrations" className="btn btn-outline">
            Manage Integrations
          </Link>
        </div>
      </div>

      {error && <div className="error-banner">⚠ {error}</div>}

      {!error && (
        loading ? (
          <div className="doc-grid">
            {[1, 2, 3].map(i => (
              <div key={i} className="skeleton" style={{ height: 140, borderRadius: 'var(--radius-lg)' }} />
            ))}
          </div>
        ) : services.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">📋</div>
            <h3>No services tracked yet</h3>
            <p>Add your subscriptions, warranties, and services to track renewal dates and obligations automatically.</p>
            <div style={{ marginTop: 'var(--space-lg)', display: 'flex', gap: '1rem', justifyContent: 'center' }}>
              <Link to="/services/new" className="btn btn-primary">
                Add a Service
              </Link>
              <Link to="/integrations" className="btn btn-outline">
                Browse Integrations
              </Link>
            </div>
          </div>
        ) : (
          <>
            {connectedServices.length > 0 && (
              <div style={{ marginBottom: 'var(--space-2xl)' }}>
                <h2>Connected Services</h2>
                <div className="doc-grid">
                  {connectedServices.map(s => (
                    <ServiceCard key={s.id} service={s} />
                  ))}
                </div>
              </div>
            )}

            <div>
              <h2>{connectedServices.length > 0 ? 'Manual Entries' : 'Your Services'}</h2>
              <div className="doc-grid">
                {manualServices.map(s => (
                  <ServiceCard key={s.id} service={s} />
                ))}
              </div>
            </div>
          </>
        )
      )}
    </>
  );
};
