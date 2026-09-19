import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getConnections, disconnectProvider, getCatalog, connectProvider, syncProvider } from '../api/integrationsApi';
import type { ProviderConnectionDto, ProviderCatalogDto } from '../types';
import { useToast } from '../components/Toast';

export const IntegrationsPage: React.FC = () => {
  const [connections, setConnections] = useState<ProviderConnectionDto[]>([]);
  const [catalog, setCatalog] = useState<ProviderCatalogDto[]>([]);
  const [error, setError] = useState('');
  
  // Modal state
  const [selectedProvider, setSelectedProvider] = useState<ProviderCatalogDto | null>(null);
  const [credentials, setCredentials] = useState<Record<string, string>>({});
  const [connecting, setConnecting] = useState(false);

  const { addToast } = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    getConnections()
      .then(setConnections)
      .catch(err => setError(err instanceof Error ? err.message : 'Failed to load connections.'));
      
    getCatalog()
      .then(setCatalog)
      .catch(err => console.error('Failed to load catalog:', err));
  }, []);

  const handleDisconnect = async (id: string, providerName: string) => {
    if (!window.confirm(`Are you sure you want to disconnect ${providerName}?`)) return;
    try {
      await disconnectProvider(id);
      setConnections(prev => prev.filter(c => c.id !== id));
      addToast(`Disconnected from ${providerName}`, 'success');
    } catch (err) {
      addToast(err instanceof Error ? err.message : `Failed to disconnect ${providerName}`, 'error');
    }
  };
  
  const handleSync = async (id: string) => {
    try {
      const updated = await syncProvider(id);
      setConnections(prev => prev.map(c => c.id === id ? updated : c));
      addToast(`Sync successful`, 'success');
    } catch (err) {
      addToast(err instanceof Error ? err.message : `Failed to sync`, 'error');
    }
  };

  const handleConnect = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProvider) return;
    
    setConnecting(true);
    try {
      const newConnection = await connectProvider({
        providerName: selectedProvider.id,
        credentials
      });
      setConnections(prev => [...prev.filter(c => c.providerName !== selectedProvider.id), newConnection]);
      addToast(`Connected to ${selectedProvider.displayName}`, 'success');
      setSelectedProvider(null);
      setCredentials({});
    } catch (err) {
      addToast(err instanceof Error ? err.message : `Failed to connect`, 'error');
    } finally {
      setConnecting(false);
    }
  };

  const openConnectModal = (provider: ProviderCatalogDto) => {
    setSelectedProvider(provider);
    setCredentials({});
  };

  const fillDemoCredentials = () => {
    if (!selectedProvider) return;
    if (selectedProvider.id === 'ELECTRICITY_BOARD') setCredentials({ accountNumber: 'DEMO-123456' });
    else if (selectedProvider.id === 'FLIGHT_SYNC') setCredentials({ pnr: 'XYZ987' });
    else if (selectedProvider.id === 'GOV_TAX') setCredentials({ ssn: 'XXX-XX-1234' });
    else if (selectedProvider.id === 'GOV_ID') setCredentials({ aadhaar: 'DEMO-XXXX' });
    else if (selectedProvider.id === 'BROADBAND') setCredentials({ customerId: 'DEMO-NET' });
    else if (selectedProvider.id === 'STREAMING') setCredentials({ email: 'demo@example.com' });
    else setCredentials({ testAccount: 'DEMO' });
  };

  const handleAddManually = (providerName: string) => {
    navigate('/services/new', { state: { presetName: providerName } });
  };

  // Group providers by category
  const categories = [...new Set(catalog.map(p => p.category))];

  return (
    <>
      <div className="page-header">
        <h1>Connected Services</h1>
        <p>Connect third-party accounts or add services manually to track subscriptions, warranties, and obligations.</p>
      </div>

      {error && <div className="error-banner">⚠ {error}</div>}

      {/* Active Connections — only shows REAL connections */}
      {connections.length > 0 && (
        <section style={{ marginBottom: 'var(--space-2xl)' }}>
          <h2>Active Connections</h2>
          <div className="card" style={{ marginTop: 'var(--space-md)', padding: 0, overflow: 'hidden' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead style={{ backgroundColor: 'var(--surface-sunken)', borderBottom: '1px solid var(--border-color)' }}>
                <tr>
                  <th style={{ padding: 'var(--space-sm) var(--space-md)', textAlign: 'left' }}>Provider</th>
                  <th style={{ padding: 'var(--space-sm) var(--space-md)', textAlign: 'left' }}>Account</th>
                  <th style={{ padding: 'var(--space-sm) var(--space-md)', textAlign: 'left' }}>Status</th>
                  <th style={{ padding: 'var(--space-sm) var(--space-md)', textAlign: 'left' }}>Last Synced</th>
                  <th style={{ padding: 'var(--space-sm) var(--space-md)', textAlign: 'right' }}></th>
                </tr>
              </thead>
              <tbody>
                {connections.map(c => (
                  <tr key={c.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: 'var(--space-md)' }}><strong>{c.providerName}</strong></td>
                    <td style={{ padding: 'var(--space-md)' }}>{c.providerAccountId || 'N/A'}</td>
                    <td style={{ padding: 'var(--space-md)' }}>
                      <span className="badge" style={{ backgroundColor: c.status === 'CONNECTED' ? 'var(--success)' : 'var(--warning)', color: 'white' }}>
                        {c.status}
                      </span>
                    </td>
                    <td style={{ padding: 'var(--space-md)' }}>
                      {c.lastSyncedAt ? new Date(c.lastSyncedAt).toLocaleString() : 'Never'}
                    </td>
                    <td style={{ padding: 'var(--space-md)', textAlign: 'right' }}>
                      <button
                        className="btn btn-ghost"
                        style={{ padding: '0.25rem 0.5rem', fontSize: '0.875rem', marginRight: '8px' }}
                        onClick={() => handleSync(c.id)}
                      >
                        Sync Now
                      </button>
                      <button
                        className="btn btn-ghost"
                        style={{ padding: '0.25rem 0.5rem', fontSize: '0.875rem' }}
                        onClick={() => handleDisconnect(c.id, c.providerName)}
                      >
                        Disconnect
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* Provider Directory */}
      <section>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-md)' }}>
          <h2>Provider Directory</h2>
          <button className="btn btn-primary" onClick={() => navigate('/services/new')}>
            + Add Custom Service
          </button>
        </div>

        {categories.map(category => (
          <div key={category} style={{ marginBottom: 'var(--space-xl)' }}>
            <h3 style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 'var(--space-sm)' }}>
              {category}
            </h3>
            <div className="doc-grid">
              {catalog.filter(p => p.category === category).map(provider => {
                const isConnected = connections.some(c => c.providerName === provider.id);
                
                return (
                  <div key={provider.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-md)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-sm)' }}>
                      <div>
                        <h3 style={{ margin: 0 }}>{provider.displayName}</h3>
                      </div>
                    </div>
                    
                    <p style={{ color: 'var(--text-secondary)', flex: 1, fontSize: '0.9rem' }}>{provider.description}</p>
                    
                    {isConnected ? (
                      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-sm)', marginTop: 'auto' }}>
                        <span className="badge" style={{ backgroundColor: 'var(--success)', color: 'white' }}>
                          ✓ Connected
                        </span>
                      </div>
                    ) : (
                      <button className="btn btn-primary" style={{ marginTop: 'auto' }} onClick={() => openConnectModal(provider)}>
                        Connect Account
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </section>

      {/* Connect Modal */}
      {selectedProvider && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 }}>
          <div className="card" style={{ width: '100%', maxWidth: 450, padding: 'var(--space-xl)' }}>
            <h2>Connect to {selectedProvider.displayName}</h2>
            <p style={{ color: 'var(--text-secondary)', marginBottom: 'var(--space-lg)', fontSize: '0.9rem' }}>
              {selectedProvider.description}
            </p>
            
            <form onSubmit={handleConnect}>
              <div className="form-group">
                <label>{selectedProvider.id === 'todoist' || selectedProvider.id === 'google_calendar' ? 'API Token / API Key' : 'Identifier / Account No.'}</label>
                <input 
                  type="text" 
                  className="form-control" 
                  placeholder={selectedProvider.id === 'todoist' ? 'Enter Todoist API Token' : 'Enter Account ID'}
                  value={Object.values(credentials)[0] || ''}
                  onChange={e => setCredentials({ 
                    [selectedProvider.id === 'ELECTRICITY_BOARD' ? 'accountNumber' : 
                     selectedProvider.id === 'FLIGHT_SYNC' ? 'pnr' : 
                     selectedProvider.id === 'GOV_TAX' ? 'ssn' : 
                     selectedProvider.id === 'todoist' ? 'token' : 'accountId']: e.target.value 
                  })}
                  required
                />
              </div>

              <div style={{ display: 'flex', gap: 'var(--space-sm)', marginTop: 'var(--space-xl)' }}>
                <button type="button" className="btn btn-outline" onClick={fillDemoCredentials}>
                  ⚡ Quick Demo
                </button>
                <div style={{ flex: 1 }}></div>
                <button type="button" className="btn btn-ghost" onClick={() => setSelectedProvider(null)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={connecting}>
                  {connecting ? 'Connecting...' : 'Connect'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
};
