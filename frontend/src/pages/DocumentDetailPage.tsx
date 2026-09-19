import React, { useEffect, useState, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getDocument, reprocessDocument, deleteDocument } from '../api/documentsApi';
import type { DocumentDetailDto, ActionDto, TimelineItem } from '../types';
import { formatDate, formatDateTime, formatFileSize, formatDocType, formatKeyDateType, docTypeIcon } from '../utils';
import { TimelineCard } from '../components/TimelineCard';
import { EvidenceChain, EvidenceStep } from '../components/EvidenceChain';
import { useToast } from '../components/Toast';

export const DocumentDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [doc, setDoc] = useState<DocumentDetailDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reprocessing, setReprocessing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const { addToast } = useToast();

  const fetchDoc = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError('');
    try {
      const data = await getDocument(id);
      setDoc(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load document.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { fetchDoc(); }, [fetchDoc]);

  const handleReprocess = async () => {
    if (!id || reprocessing) return;
    setReprocessing(true);
    try {
      await reprocessDocument(id);
      addToast('Reprocessing started. Refreshing...', 'info');
      setTimeout(fetchDoc, 2000);
    } catch (err) {
      addToast(err instanceof Error ? err.message : 'Reprocess failed.', 'error');
    } finally {
      setReprocessing(false);
    }
  };

  const handleDelete = async () => {
    if (!id || !window.confirm('Are you sure you want to delete this document? All extracted data and actions will be removed.')) return;
    setDeleting(true);
    try {
      await deleteDocument(id);
      addToast('Document deleted.', 'success');
      navigate('/documents');
    } catch (err) {
      addToast(err instanceof Error ? err.message : 'Failed to delete document.', 'error');
      setDeleting(false);
    }
  };

  const handleActionCompleted = () => {
    fetchDoc(); // Just refetch to keep it simple and accurate
  };

  if (loading) {
    return (
      <div style={{ maxWidth: '800px', margin: '0 auto' }}>
        <Link to="/documents" className="back-link">← Documents</Link>
        <div className="skeleton" style={{ height: 32, width: 200, marginBottom: 'var(--space-md)' }} />
        <div className="skeleton" style={{ height: 200, marginBottom: 'var(--space-lg)' }} />
        <div className="skeleton" style={{ height: 300 }} />
      </div>
    );
  }

  if (error || !doc) {
    return (
      <div style={{ maxWidth: '800px', margin: '0 auto' }}>
        <Link to="/documents" className="back-link">← Documents</Link>
        <div className="error-banner">⚠ {error || 'Document not found.'}</div>
      </div>
    );
  }

  // Convert ActionDto to TimelineItem for the TimelineCard
  const toTimelineItem = (a: ActionDto): TimelineItem => ({
    id: `action-${a.id}`,
    source: 'document',
    title: a.title,
    subtitle: a.description ?? '',
    deadline: a.deadline ?? a.recommendedDate,
    daysRemaining: null, // Doesn't matter here, we don't show urgency groupings on this page
    priority: a.priority,
    status: a.status,
    reason: a.reason,
    evidence: a.evidence,
    documentId: a.documentId,
    serviceId: null,
    officialUrl: null,
    action: a,
  });

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <Link to="/documents" className="back-link">← Documents</Link>

      {/* Document Header */}
      <div className="card" style={{ marginBottom: 'var(--space-xl)' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 'var(--space-md)' }}>
          <div className="doc-card-icon" style={{ fontSize: '1.5rem', width: 52, height: 52, flexShrink: 0 }}>
            {docTypeIcon(doc.documentType)}
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <h1 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: 'var(--space-xs)', wordBreak: 'break-word' }}>
              {doc.originalFilename}
            </h1>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-sm)', fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
              <span className={`status-badge status-${doc.processingStatus}`}>{doc.processingStatus}</span>
              <span>{formatDocType(doc.documentType)}</span>
              <span>{formatFileSize(doc.fileSize)}</span>
              <span>Uploaded {formatDateTime(doc.createdAt)}</span>
            </div>
          </div>
          {(doc.processingStatus === 'FAILED' || doc.processingStatus === 'NEEDS_REVIEW') && (
            <button className="btn btn-primary btn-sm" onClick={handleReprocess} disabled={reprocessing}>
              {reprocessing ? 'Reprocessing…' : '↻ Reprocess'}
            </button>
          )}
        </div>
      </div>

      {/* Summary */}
      {doc.summary && (
        <div className="doc-detail-section">
          <h3>AI Summary</h3>
          <div className="card">
            <p style={{ fontSize: '0.9375rem', lineHeight: 1.7, color: 'var(--text-secondary)' }}>
              {doc.summary}
            </p>
            <div style={{ marginTop: 'var(--space-sm)', fontSize: '0.6875rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              AI Interpretation
            </div>
          </div>
        </div>
      )}

      <div className="doc-detail-grid">
        {/* Key Dates */}
        <div className="doc-detail-section">
          <h3>Key Dates</h3>
          {doc.keyDates.length === 0 ? (
            <p style={{ fontSize: '0.875rem', color: 'var(--text-tertiary)' }}>No key dates extracted.</p>
          ) : (
            doc.keyDates.map(kd => (
              <div key={kd.id} className="key-date-item">
                <div>
                  <div className="key-date-label">{formatKeyDateType(kd.dateType)}</div>
                  {kd.description && (
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: '0.125rem' }}>
                      {kd.description}
                    </div>
                  )}
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div className="key-date-value">{formatDate(kd.dateValue)}</div>
                  {kd.confidence != null && (
                    <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>
                      {Math.round(kd.confidence * 100)}% confidence
                    </div>
                  )}
                </div>
              </div>
            ))
          )}
        </div>

        {/* Obligations */}
        <div className="doc-detail-section">
          <h3>Obligations</h3>
          {doc.obligations.length === 0 ? (
            <p style={{ fontSize: '0.875rem', color: 'var(--text-tertiary)' }}>No obligations identified.</p>
          ) : (
            doc.obligations.map(obl => (
              <div key={obl.id} className="obligation-item">
                <div className="obligation-type">{obl.obligationType.replace(/_/g, ' ')}</div>
                <div className="obligation-desc">{obl.description}</div>
                {obl.evidence && (
                  <div className="obligation-evidence">"{obl.evidence}"</div>
                )}
                {obl.confidence != null && (
                  <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginTop: 'var(--space-xs)' }}>
                    {Math.round(obl.confidence * 100)}% confidence · Document fact
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>

      {/* Related Actions */}
      <div className="doc-detail-section" style={{ marginTop: 'var(--space-xl)' }}>
        <h3>Generated Actions & Evidence</h3>
        {doc.actions.length === 0 ? (
          <p style={{ fontSize: '0.875rem', color: 'var(--text-tertiary)' }}>No actions generated from this document.</p>
        ) : (
          <div>
            {doc.actions.map(action => {
              // Reconstruct the evidence chain visually
              const obl = doc.obligations.find(o => o.id === action.obligationId);
              const steps: EvidenceStep[] = [];
              
              if (obl) {
                steps.push({
                  type: 'fact',
                  label: 'Document Fact',
                  text: obl.description,
                  quote: obl.evidence || undefined
                });
              }
              
              if (action.reason) {
                steps.push({
                  type: 'rule',
                  label: 'Rule / Context',
                  text: action.reason
                });
              }

              if (action.recommendedDate) {
                steps.push({
                  type: 'calc',
                  label: 'System Calculation',
                  text: `Action recommended by ${formatDate(action.recommendedDate)}`
                });
              }

              steps.push({
                type: 'action',
                label: 'Action Required',
                text: action.title
              });

              return (
                <div key={action.id} style={{ marginBottom: 'var(--space-2xl)' }}>
                  <TimelineCard item={toTimelineItem(action)} onActionCompleted={handleActionCompleted} />
                  <div style={{ marginLeft: 'var(--space-lg)' }}>
                    <EvidenceChain steps={steps} />
                  </div>
                </div>
              );
            })}
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
          {deleting ? 'Deleting...' : 'Delete Document'}
        </button>
      </div>

    </div>
  );
};
