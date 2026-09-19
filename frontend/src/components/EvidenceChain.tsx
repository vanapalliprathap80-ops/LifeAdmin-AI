import React from 'react';

export interface EvidenceStep {
  type: 'fact' | 'rule' | 'calc' | 'action' | 'user';
  label: string;
  text: string;
  quote?: string;
}

interface EvidenceChainProps {
  steps: EvidenceStep[];
}

export const EvidenceChain: React.FC<EvidenceChainProps> = ({ steps }) => {
  if (!steps || steps.length === 0) return null;

  return (
    <div className="evidence-chain">
      {steps.map((step, idx) => (
        <div key={idx} className="evidence-step">
          {idx < steps.length - 1 && <div className="evidence-connector" />}
          
          <div className={`evidence-dot ${step.type}`}>
            {step.type === 'fact' && '📄'}
            {step.type === 'rule' && '⚖️'}
            {step.type === 'calc' && '⚙️'}
            {step.type === 'action' && '🎯'}
            {step.type === 'user' && '👤'}
          </div>
          
          <div className="evidence-step-content">
            <div className={`evidence-step-label ${step.type}`}>{step.label}</div>
            <div className="evidence-step-text">{step.text}</div>
            {step.quote && <div className="evidence-quote">"{step.quote}"</div>}
          </div>
        </div>
      ))}
    </div>
  );
};
