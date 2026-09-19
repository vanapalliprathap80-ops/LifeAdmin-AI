import React from 'react';
import { Link } from 'react-router-dom';
import type { ServiceDto } from '../types';
import { formatPrice, formatRelative, urgencyColor, daysRemaining } from '../utils';

interface ServiceCardProps {
  service: ServiceDto;
}

export const ServiceCard: React.FC<ServiceCardProps> = ({ service }) => {
  const days = daysRemaining(service.renewalDate);
  const color = urgencyColor(days);

  return (
    <Link to={`/services/${service.id}`} className="service-card">
      <h4 className="service-card-name">{service.assetName || service.name}</h4>
      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'capitalize', marginBottom: '4px' }}>
        {service.serviceType?.toLowerCase() || 'Subscription'} {service.providerConnectionId ? ' (Connected)' : ''}
      </div>
      {service.currentPlan && (
        <div className="service-card-plan">{service.currentPlan}</div>
      )}
      <div className="service-card-price">
        {service.price ? formatPrice(service.price, service.billingFrequency) : '—'}
      </div>
      
      <div className="service-card-meta">
        {service.renewalDate && (
          <span style={{ color }}>
            {service.serviceType === 'WARRANTY' ? '⚠ Expires ' : '↻ Renews '}{formatRelative(service.renewalDate)}
          </span>
        )}
      </div>
    </Link>
  );
};
