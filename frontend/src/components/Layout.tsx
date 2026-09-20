import React, { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { setAuthToken, apiFetch } from '../api/client';

export const Layout: React.FC = () => {
  const [userEmail, setUserEmail] = useState<string>('');
  const navigate = useNavigate();

  useEffect(() => {
    // Fetch current user info
    apiFetch<{ email: string; id: string }>('/api/auth/me')
      .then(data => setUserEmail(data.email))
      .catch(() => setUserEmail(''));
  }, []);

  const handleLogout = () => {
    setAuthToken(null);
    navigate('/login');
  };

  const isDemo = userEmail === 'demo@lifeadmin.ai';

  return (
    <div className="app-layout">
      <header className="app-header">
        <NavLink to="/" className="app-logo" aria-label="LifeAdmin AI home">
          <div className="app-logo-icon">✦</div>
          LifeAdmin
          <span className="ai-badge">AI</span>
        </NavLink>
        <nav className="app-nav" aria-label="Main navigation">
          <NavLink to="/" end className={({ isActive }) => `app-nav-link${isActive ? ' active' : ''}`}>
            Action Center
          </NavLink>
          <NavLink to="/services" className={({ isActive }) => `app-nav-link${isActive ? ' active' : ''}`}>
            Services
          </NavLink>
          <NavLink to="/documents" className={({ isActive }) => `app-nav-link${isActive ? ' active' : ''}`}>
            Documents
          </NavLink>
          <NavLink to="/integrations" className={({ isActive }) => `app-nav-link${isActive ? ' active' : ''}`}>
            Integrations
          </NavLink>
          <NavLink to="/notifications" className={({ isActive }) => `app-nav-link${isActive ? ' active' : ''}`}>
            Notifications
          </NavLink>
          <NavLink to="/chat" className={({ isActive }) => `app-nav-link${isActive ? ' active' : ''}`}>
            ✦ AI Assistant
          </NavLink>
        </nav>
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '12px' }}>
          {userEmail && (
            <span style={{ 
              fontSize: '0.75rem', 
              padding: '4px 10px', 
              borderRadius: '20px', 
              background: 'var(--bg-tertiary, #222)', 
              color: 'var(--text-secondary, #aaa)',
              border: '1px solid var(--border, #333)',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}>
              <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#10b981' }}></span>
              {isDemo ? 'Demo User' : userEmail}
            </span>
          )}
          <button 
            className="btn btn-ghost" 
            onClick={handleLogout}
            style={{ padding: '4px 12px', fontSize: '0.8rem' }}
          >
            Logout
          </button>
        </div>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
};
