import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { completeAction } from '../api/actionsApi';
import type { TimelineItem } from '../types';
import { formatDate, formatRelative, urgencyColor, urgencyDot } from '../utils';
import { useToast } from './Toast';

interface TimelineCardProps {
  item: TimelineItem;
  onActionCompleted?: () => void;
}

export const TimelineCard: React.FC<TimelineCardProps> = ({ item, onActionCompleted }) => {
  const [completing, setCompleting] = useState(false);
  const { addToast } = useToast();
  const isCompleted = item.status === 'COMPLETED';
  
  const color = urgencyColor(item.daysRemaining);
  const dot = urgencyDot(item.daysRemaining);

  const handleComplete = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (completing || isCompleted || item.source !== 'document' || !item.action) return;
    
    setCompleting(true);
    try {
      await completeAction(item.action.id);
      addToast(`"${item.title}" marked complete!`, 'success');
      if (onActionCompleted) onActionCompleted();
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Could not complete action.';
      addToast(msg, 'error');
    } finally {
      setCompleting(false);
    }
  };

  return (
    <div className={`timeline-card priority-${item.priority}${isCompleted ? ' completed' : ''}`}>
      <div className="timeline-card-top">
        <div>
          <div className="timeline-card-urgency">
            <span className="timeline-card-urgency-dot">{dot}</span>
            <span className={`timeline-card-source source-${item.source}`}>
              {item.source === 'document' ? '📄 Document' : '📋 Service'}
            </span>
          </div>
          <h4 className="timeline-card-title">{item.title}</h4>
        </div>
        <span className={`priority-badge priority-${item.priority}`}>
          {item.priority === 'CRITICAL' && '⚠ '}{item.priority}
        </span>
      </div>

      <div className="timeline-card-subtitle">{item.subtitle}</div>

      {item.deadline && (
        <div className="timeline-card-deadline" style={{ color }}>
          {item.daysRemaining !== null && item.daysRemaining <= 0 ? 'Due ' : 'Due '}
          {formatDate(item.deadline)} 
          <span style={{ color: 'var(--text-tertiary)', marginLeft: '0.25rem', fontWeight: 400 }}>
            ({formatRelative(item.deadline)})
          </span>
        </div>
      )}

      {/* Why / Explainability */}
      {item.reason && (
        <div className="why-section">
          <div className="why-label">Why am I seeing this?</div>
          <div className="why-content">{item.reason}</div>
        </div>
      )}

      {/* Decision Moment UX */}
      {!isCompleted && (
        <div className="decision-moment">
          <div className="decision-moment-label">Decision needed</div>
          <div className="decision-moment-actions">
            
            {item.source === 'document' && (
              <>
                <Link to={`/documents/${item.documentId}`} className="btn btn-primary btn-sm">
                  Review Document
                </Link>
                <button
                  className="btn btn-ghost btn-sm"
                  onClick={handleComplete}
                  disabled={completing}
                >
                  {completing ? 'Completing…' : '✓ Mark Done'}
                </button>
              </>
            )}

            {item.source === 'service' && (
              <>
                <Link to={`/services/${item.serviceId}`} className="btn btn-primary btn-sm">
                  Review Options
                </Link>
                {item.officialUrl && (
                  <a href={item.officialUrl} target="_blank" rel="noopener noreferrer" className="official-link">
                    Open Official Site ↗
                  </a>
                )}
              </>
            )}

          </div>
        </div>
      )}
      
      {isCompleted && (
        <div style={{ marginTop: 'var(--space-md)', fontSize: '0.8125rem', color: 'var(--success)', fontWeight: 600 }}>
          ✓ Action completed
        </div>
      )}
    </div>
  );
};
