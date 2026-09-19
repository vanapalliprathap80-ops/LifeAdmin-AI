import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { createService } from '../api/servicesApi';
import { useToast } from '../components/Toast';
import type { CreateServiceRequest } from '../types';

export const AddServicePage: React.FC = () => {
  const [formData, setFormData] = useState<CreateServiceRequest>({ name: '' });
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const { addToast } = useToast();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value === '' ? undefined : value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await createService(formData);
      addToast('Service added successfully.', 'success');
      navigate('/services');
    } catch (err) {
      addToast(err instanceof Error ? err.message : 'Failed to add service.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto' }}>
      <Link to="/services" className="back-link">← Back to Services</Link>
      
      <div className="page-header" style={{ marginBottom: 'var(--space-xl)' }}>
        <h1>Add a Service</h1>
        <p>Manually track a subscription or service.</p>
      </div>

      <div className="card">
        <form onSubmit={handleSubmit}>
          
          <div className="form-group">
            <label className="form-label" htmlFor="name">Service Name *</label>
            <input
              type="text"
              id="name"
              name="name"
              className="form-input"
              required
              placeholder="e.g., Netflix, Gym Membership"
              value={formData.name || ''}
              onChange={handleChange}
            />
          </div>

          <div className="doc-detail-grid">
            <div className="form-group">
              <label className="form-label" htmlFor="currentPlan">Current Plan</label>
              <input
                type="text"
                id="currentPlan"
                name="currentPlan"
                className="form-input"
                placeholder="e.g., Standard, Premium"
                value={formData.currentPlan || ''}
                onChange={handleChange}
              />
            </div>
            
            <div className="form-group">
              <label className="form-label" htmlFor="price">Price</label>
              <input
                type="number"
                step="0.01"
                id="price"
                name="price"
                className="form-input"
                placeholder="0.00"
                value={formData.price || ''}
                onChange={handleChange}
              />
            </div>
          </div>

          <div className="doc-detail-grid">
            <div className="form-group">
              <label className="form-label" htmlFor="billingFrequency">Billing Frequency</label>
              <select
                id="billingFrequency"
                name="billingFrequency"
                className="form-select"
                value={formData.billingFrequency || ''}
                onChange={handleChange}
              >
                <option value="">Select frequency...</option>
                <option value="MONTHLY">Monthly</option>
                <option value="QUARTERLY">Quarterly</option>
                <option value="HALF_YEARLY">Every 6 Months</option>
                <option value="YEARLY">Yearly</option>
                <option value="ONE_TIME">One Time</option>
              </select>
            </div>
            
            <div className="form-group">
              <label className="form-label" htmlFor="renewalDate">Next Renewal Date</label>
              <input
                type="date"
                id="renewalDate"
                name="renewalDate"
                className="form-input"
                value={formData.renewalDate || ''}
                onChange={handleChange}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="officialUrl">Official Website URL</label>
            <input
              type="url"
              id="officialUrl"
              name="officialUrl"
              className="form-input"
              placeholder="https://..."
              value={formData.officialUrl || ''}
              onChange={handleChange}
            />
            <div className="form-hint">Provide the official link to manage your subscription.</div>
          </div>

          <div className="form-group" style={{ marginTop: 'var(--space-2xl)' }}>
            <button type="submit" className="btn btn-primary" disabled={submitting} style={{ width: '100%' }}>
              {submitting ? 'Adding...' : 'Add Service'}
            </button>
          </div>
          
        </form>
      </div>
    </div>
  );
};
