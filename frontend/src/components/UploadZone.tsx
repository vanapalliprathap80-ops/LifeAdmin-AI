import React, { useState, useRef, useCallback } from 'react';
import { uploadDocument } from '../api/documentsApi';
import type { DocumentDto } from '../types';
import { useToast } from './Toast';

interface UploadZoneProps {
  onUploadComplete: (doc: DocumentDto) => void;
}

type UploadStage = 'idle' | 'uploading' | 'reading' | 'analyzing' | 'done' | 'error';

const STAGES: { key: UploadStage; label: string }[] = [
  { key: 'uploading', label: 'Uploading document' },
  { key: 'reading', label: 'Reading & extracting text' },
  { key: 'analyzing', label: 'AI analyzing obligations & deadlines' },
  { key: 'done', label: 'Creating your action plan' },
];

export const UploadZone: React.FC<UploadZoneProps> = ({ onUploadComplete }) => {
  const [stage, setStage] = useState<UploadStage>('idle');
  const [dragOver, setDragOver] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [fileName, setFileName] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);
  const { addToast } = useToast();

  const processFile = useCallback(async (file: File) => {
    // Validate
    if (file.type !== 'application/pdf') {
      setErrorMsg('Please upload a PDF file.');
      addToast('Only PDF files are supported.', 'error');
      return;
    }
    if (file.size > 20 * 1024 * 1024) {
      setErrorMsg('File size must be under 20 MB.');
      addToast('File too large. Maximum size is 20 MB.', 'error');
      return;
    }

    setErrorMsg('');
    setFileName(file.name);
    setStage('uploading');

    // Simulate stage progression while the backend processes
    const timer1 = setTimeout(() => setStage('reading'), 800);
    const timer2 = setTimeout(() => setStage('analyzing'), 2000);

    try {
      const doc = await uploadDocument(file);
      clearTimeout(timer1);
      clearTimeout(timer2);
      setStage('done');
      addToast('Document processed successfully!', 'success');
      setTimeout(() => {
        onUploadComplete(doc);
        setStage('idle');
        setFileName('');
      }, 1200);
    } catch (err) {
      clearTimeout(timer1);
      clearTimeout(timer2);
      const msg = err instanceof Error ? err.message : 'Upload failed. Please try again.';
      setErrorMsg(msg);
      setStage('error');
      addToast(msg, 'error');
      setTimeout(() => setStage('idle'), 3000);
    }
  }, [onUploadComplete, addToast]);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) processFile(file);
  }, [processFile]);

  const handleFileChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) processFile(file);
    e.target.value = '';
  }, [processFile]);

  if (stage !== 'idle' && stage !== 'error') {
    return (
      <div className="upload-progress" role="status" aria-live="polite">
        <div className="upload-progress-header">
          {stage !== 'done' ? (
            <div className="upload-spinner" aria-label="Processing" />
          ) : (
            <div style={{
              width: 40, height: 40, borderRadius: '50%',
              background: 'var(--success-subtle)', display: 'flex',
              alignItems: 'center', justifyContent: 'center',
              fontSize: '1.25rem', color: 'var(--success)'
            }}>✓</div>
          )}
          <div>
            <div style={{ fontWeight: 600, fontSize: '0.9375rem' }}>
              {stage === 'done' ? 'Processing complete!' : 'Processing your document'}
            </div>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>{fileName}</div>
          </div>
        </div>
        <div className="upload-steps">
          {STAGES.map(s => {
            const stageIdx = STAGES.findIndex(st => st.key === stage);
            const thisIdx = STAGES.findIndex(st => st.key === s.key);
            const isDone = thisIdx < stageIdx || stage === 'done';
            const isActive = thisIdx === stageIdx && stage !== 'done';
            return (
              <div key={s.key} className={`upload-step${isActive ? ' active' : ''}${isDone ? ' done' : ''}`}>
                <span className="upload-step-icon">{isDone ? '✓' : isActive ? '●' : '○'}</span>
                <span>{s.label}</span>
              </div>
            );
          })}
        </div>
      </div>
    );
  }

  return (
    <div
      className={`upload-zone${dragOver ? ' drag-over' : ''}`}
      onDragOver={e => { e.preventDefault(); setDragOver(true); }}
      onDragLeave={() => setDragOver(false)}
      onDrop={handleDrop}
      onClick={() => inputRef.current?.click()}
      role="button"
      tabIndex={0}
      aria-label="Upload PDF document"
      onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') inputRef.current?.click(); }}
    >
      <input
        ref={inputRef}
        type="file"
        accept="application/pdf"
        onChange={handleFileChange}
        aria-label="Choose PDF file"
      />
      <div className="upload-icon">📄</div>
      <h3>{dragOver ? 'Drop your PDF here' : 'Drop a PDF or click to browse'}</h3>
      <p>LifeAdmin AI reads your document, finds obligations, and creates your action plan.</p>
      <div className="file-types">Supports PDF files up to 20 MB</div>
      {errorMsg && (
        <div style={{
          marginTop: 'var(--space-md)', padding: '0.5rem 1rem',
          background: 'var(--critical-subtle)', color: 'var(--critical)',
          borderRadius: 'var(--radius-md)', fontSize: '0.8125rem'
        }}>
          {errorMsg}
        </div>
      )}
    </div>
  );
};
