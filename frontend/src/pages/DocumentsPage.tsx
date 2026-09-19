import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { listDocuments } from '../api/documentsApi';
import type { DocumentDto } from '../types';
import { formatDateTime, formatFileSize, formatDocType, docTypeIcon } from '../utils';

export const DocumentsPage: React.FC = () => {
  const [docs, setDocs] = useState<DocumentDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchDocs = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await listDocuments();
      setDocs(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load documents.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchDocs(); }, [fetchDocs]);

  return (
    <>
      <div className="page-header">
        <h1>Documents</h1>
        <p>Your uploaded documents and their AI analysis results.</p>
      </div>

      {error && <div className="error-banner">⚠ {error}</div>}

      {!error && (
        loading ? (
          <div className="doc-grid">
            {[1, 2, 3].map(i => (
              <div key={i} className="skeleton" style={{ height: 140, borderRadius: 'var(--radius-lg)' }} />
            ))}
          </div>
        ) : docs.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">📂</div>
            <h3>No documents yet</h3>
            <p>Head to the Action Center to upload your first document.</p>
            <Link to="/" className="btn btn-primary" style={{ marginTop: 'var(--space-lg)', display: 'inline-flex' }}>
              Upload a document
            </Link>
          </div>
        ) : (
          <div className="doc-grid">
            {docs.map(doc => (
              <Link key={doc.id} to={`/documents/${doc.id}`} className="doc-card">
                <div className="doc-card-icon">{docTypeIcon(doc.documentType)}</div>
                <div className="doc-card-title">{doc.originalFilename}</div>
                <div className="doc-card-meta">
                  <span className={`status-badge status-${doc.processingStatus}`}>
                    {doc.processingStatus}
                  </span>
                  <span>{formatDocType(doc.documentType)}</span>
                  <span>{formatFileSize(doc.fileSize)}</span>
                  <span>{formatDateTime(doc.createdAt)}</span>
                </div>
                {doc.summary && (
                  <p style={{
                    marginTop: 'var(--space-sm)', fontSize: '0.8125rem',
                    color: 'var(--text-tertiary)', lineHeight: 1.5,
                    display: '-webkit-box', WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical', overflow: 'hidden'
                  }}>
                    {doc.summary}
                  </p>
                )}
              </Link>
            ))}
          </div>
        )
      )}
    </>
  );
};
