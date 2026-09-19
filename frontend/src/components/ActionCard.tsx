import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { completeAction } from '../api/actionsApi';
import type { ActionDto } from '../types';
import { formatDate, formatRelative } from '../utils';
import { useToast } from './Toast';

interface ActionCardProps {
  action: ActionDto;
  onCompleted: (updated: ActionDto) => void;
}

export const ActionCard: React.FC<ActionCardProps> = ({ action, onCompleted }) => {
  const [completing, setCompleting] = useState(false);
  const { addToast } = useToast();
  const isCompleted = action.status === 'COMPLETED';

  const handleComplete = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (completing || isCompleted) return;
    setCompleting(true);
    try {
      const updated = await completeAction(action.id);
      addToast(`"${action.title}" marked complete!`, 'success');
      onCompleted(updated);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Could not complete action.';
      addToast(msg, 'error');
    } finally {
      setCompleting(false);
    }
  };

  return (
    <div className={`action-card priority-${action.priority}${isCompleted ? ' completed' : ''}`}>
      <div className="action-card-header">
        <h4 className="action-card-title">{action.title}</h4>
        <span className={`priority-badge priority-${action.priority}`}>
          {action.priority === 'CRITICAL' && '⚠ '}
          {action.priority}
        </span>
      </div>

      <div className="action-card-meta">
        {action.deadline && (
          <span className="action-card-meta-item" title="Deadline">
            📅 {formatDate(action.deadline)}
            <span style={{ color: 'var(--text-tertiary)', marginLeft: '0.25rem' }}>
              ({formatRelative(action.deadline)})
            </span>
          </span>
        )}
        {action.recommendedDate && action.recommendedDate !== action.deadline && (
          <span className="action-card-meta-item" title="Recommended action date">
            🎯 Act by {formatDate(action.recommendedDate)}
          </span>
        )}
      </div>

      {action.description && (
        <p className="action-card-description">{action.description}</p>
      )}

      {action.reason && (
        <div className="action-card-reason">
          <strong style={{ fontSize: '0.6875rem', textTransform: 'uppercase', letterSpacing: '0.05em', display: 'block', marginBottom: '0.25rem', color: 'var(--primary-hover)' }}>
            Why this matters
          </strong>
          {action.reason}
        </div>
      )}

      {action.evidence && (
        <div className="action-card-evidence">{action.evidence}</div>
      )}

      <div className="action-card-footer">
        <Link to={`/documents/${action.documentId}`} className="btn btn-ghost btn-sm" onClick={e => e.stopPropagation()}>
          View document
        </Link>

        {isCompleted ? (
          <div className="action-card-completed">
            <span>✓</span>
            <span>Completed {action.completedAt ? formatRelative(action.completedAt) : ''}</span>
          </div>
        ) : (
          <button
            className="btn btn-success btn-sm"
            onClick={handleComplete}
            disabled={completing}
            aria-label={`Mark "${action.title}" as complete`}
          >
            {completing ? 'Completing…' : '✓ Mark complete'}
          </button>
        )}
      </div>
    </div>
  );
};
