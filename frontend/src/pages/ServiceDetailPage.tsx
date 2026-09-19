import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getService, deleteService } from '../api/servicesApi';
import type { ServiceDto } from '../types';
import { formatDate, formatPrice, formatRelative, urgencyColor } from '../utils';
import { useToast } from '../components/Toast';

export const ServiceDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addToast } = useToast();
  
  const [service, setService] = useState<ServiceDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (!id) return;
    getService(id)
      .then(setService)
      .catch(err => setError(err instanceof Error ? err.message : 'Failed to load service.'))
      .finally(() => setLoading(false));
  }, [id]);

  const handleDelete = async () => {
    if (!id || !window.confirm('Are you sure you want to delete this service?')) return;
    setDeleting(true);
    try {
      await deleteService(id);
      addToast('Service deleted.', 'success');
      navigate('/services');
    } catch (err) {
      addToast(err instanceof Error ? err.message : 'Failed to delete service.', 'error');
      setDeleting(false);
    }
  };

  if (loading) {
    return (
      <div style={{ maxWidth: '800px', margin: '0 auto' }}>
        <div className="skeleton" style={{ height: 40, width: 200, marginBottom: 'var(--space-md)' }} />
        <div className="skeleton" style={{ height: 300, borderRadius: 'var(--radius-lg)' }} />
      </div>
    );
  }

  if (error || !service) {
    return <div className="error-banner">⚠ {error || 'Service not found'}</div>;
  }

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <Link to="/services" className="back-link">← Back to Services</Link>

      <div className="page-header" style={{ marginBottom: 'var(--space-xl)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-sm)' }}>
          <h1>{service.assetName || service.name}</h1>
          {service.providerConnectionId && (
            <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(34, 197, 94, 0.2)', color: 'var(--success)', padding: '2px 8px', borderRadius: '12px' }}>
              Connected Data
            </span>
          )}
          {!service.providerConnectionId && (
            <span style={{ fontSize: '0.75rem', backgroundColor: 'var(--bg-tertiary)', color: 'var(--text-secondary)', padding: '2px 8px', borderRadius: '12px' }}>
              User Provided
            </span>
          )}
        </div>
        <p>{service.assetName ? service.name : service.currentPlan}</p>
      </div>

      <div className="card">
        
        <div className="service-detail-info">
          
          <div className="service-detail-label">Type</div>
          <div className="service-detail-value" style={{ textTransform: 'capitalize' }}>
            {service.serviceType?.toLowerCase() || 'Subscription'}
          </div>

          {service.price !== null && (
            <>
              <div className="service-detail-label">Price</div>
              <div className="service-detail-value" style={{ fontSize: '1.25rem' }}>
                {formatPrice(service.price, service.billingFrequency)}
              </div>
            </>
          )}

          {service.renewalDate && (
            <>
              <div className="service-detail-label">Next Renewal</div>
              <div className="service-detail-value" style={{ color: urgencyColor(0) }}>
                {formatDate(service.renewalDate)} ({formatRelative(service.renewalDate)})
              </div>
            </>
          )}

          {service.officialUrl && (
            <>
              <div className="service-detail-label">Official Site</div>
              <div className="service-detail-value">
                <a href={service.officialUrl} target="_blank" rel="noopener noreferrer" className="official-link">
                  Open {service.name} ↗
                </a>
              </div>
            </>
          )}
        </div>

        {service.renewalDate && (
          <div className="decision-moment" style={{ marginTop: 'var(--space-2xl)' }}>
            <div className="decision-moment-label">{service.serviceType === 'WARRANTY' ? 'Warranty Decision' : 'Renewal Decision'}</div>
            <div className="decision-moment-actions">
              {service.officialUrl ? (
                <>
                  <a href={service.officialUrl} target="_blank" rel="noopener noreferrer" className="btn btn-primary">
                    Keep Plan
                  </a>
                  <a href={service.officialUrl} target="_blank" rel="noopener noreferrer" className="btn btn-ghost">
                    Explore Options
                  </a>
                  <a href={service.officialUrl} target="_blank" rel="noopener noreferrer" className="btn btn-ghost">
                    Manage / Cancel
                  </a>
                </>
              ) : (
                <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                  Add an official URL to manage this subscription directly.
                </div>
              )}
            </div>
          </div>
        )}

      </div>

      <div style={{ marginTop: 'var(--space-2xl)', textAlign: 'right' }}>
        <button 
          onClick={handleDelete} 
          disabled={deleting} 
          className="btn btn-ghost" 
          style={{ color: 'var(--critical)', borderColor: 'rgba(239, 68, 68, 0.2)' }}
        >
          {deleting ? 'Deleting...' : 'Delete Service'}
        </button>
      </div>

    </div>
  );
};
