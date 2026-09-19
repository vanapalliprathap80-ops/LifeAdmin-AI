import React, { useState, useRef, useEffect } from 'react';
import { sendChatMessage } from '../api/chatApi';
import { uploadDocument } from '../api/documentsApi';
import { useToast } from '../components/Toast';

interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

export const ChatPage: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      role: 'assistant',
      content: "Hi! I'm your LifeAdmin AI assistant. I can help you understand your documents, track deadlines, manage your obligations, and set reminders. Ask me anything or upload a document!",
      timestamp: new Date(),
    }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [recording, setRecording] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { addToast } = useToast();

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const appendUserMessage = (content: string) => {
    setMessages(prev => [...prev, { id: `user-${Date.now()}`, role: 'user', content, timestamp: new Date() }]);
  };

  const handleSend = async (messageText = input.trim()) => {
    if (!messageText || loading) return;

    appendUserMessage(messageText);
    if (messageText === input.trim()) setInput('');
    setLoading(true);

    try {
      const response = await sendChatMessage(messageText);
      setMessages(prev => [...prev, {
        id: `assistant-${Date.now()}`,
        role: 'assistant',
        content: response,
        timestamp: new Date(),
      }]);
    } catch (err) {
      setMessages(prev => [...prev, {
        id: `error-${Date.now()}`,
        role: 'assistant',
        content: "I'm sorry, I encountered an error connecting to the AI. Please try again.",
        timestamp: new Date(),
      }]);
    } finally {
      setLoading(false);
      inputRef.current?.focus();
    }
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    appendUserMessage(`Uploaded document: ${file.name}`);
    setLoading(true);
    addToast('Uploading document to your Action Center...', 'info');
    
    try {
      await uploadDocument(file);
      addToast('Document uploaded successfully!', 'success');
      // Trigger AI to talk about it
      const response = await sendChatMessage(`I just uploaded a document named ${file.name}. Can you summarize it?`);
      setMessages(prev => [...prev, {
        id: `assistant-${Date.now()}`,
        role: 'assistant',
        content: response,
        timestamp: new Date(),
      }]);
    } catch (err) {
      addToast('Failed to upload document', 'error');
      setLoading(false);
    } finally {
      setLoading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const toggleRecording = () => {
    if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
      addToast('Voice input is not supported in this browser.', 'error');
      return;
    }

    if (recording) {
      // Logic for stopping is handled implicitly by onend
      setRecording(false);
      return;
    }

    // @ts-ignore
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.interimResults = false;

    recognition.onstart = () => {
      setRecording(true);
      addToast('Listening...', 'info');
    };

    recognition.onresult = (event: any) => {
      const transcript = event.results[0][0].transcript;
      setInput(transcript);
    };

    recognition.onerror = () => {
      setRecording(false);
      addToast('Voice recognition failed.', 'error');
    };

    recognition.onend = () => {
      setRecording(false);
    };

    recognition.start();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const SUGGESTIONS = [
    "Remind me to pay rent on the 1st",
    "Summarize my uploaded documents",
    "What deadlines do I have coming up?",
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 80px)', maxWidth: '800px', margin: '0 auto' }}>
      <div className="page-header" style={{ flexShrink: 0, paddingBottom: 'var(--space-md)' }}>
        <h1>AI Assistant</h1>
        <p>Ask questions, set reminders, or upload documents directly.</p>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 'var(--space-md)', paddingBottom: 'var(--space-lg)' }}>
        {messages.map(msg => (
          <div key={msg.id} style={{ display: 'flex', justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start' }}>
            <div style={{
              maxWidth: '80%', padding: 'var(--space-md) var(--space-lg)',
              borderRadius: msg.role === 'user' ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
              background: msg.role === 'user' ? 'var(--primary)' : 'var(--surface-elevated)',
              color: msg.role === 'user' ? 'white' : 'var(--text-primary)',
              lineHeight: 1.6, fontSize: '0.95rem', whiteSpace: 'pre-wrap', boxShadow: 'var(--shadow-sm)',
            }}>
              {msg.role === 'assistant' && (
                <div style={{ fontSize: '0.7rem', color: 'var(--accent)', fontWeight: 600, marginBottom: '6px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  ✦ LifeAdmin AI
                </div>
              )}
              {msg.content}
            </div>
          </div>
        ))}
        {loading && (
          <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
            <div style={{ padding: 'var(--space-md) var(--space-lg)', borderRadius: '18px 18px 18px 4px', background: 'var(--surface-elevated)', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <div className="typing-indicator"><span></span><span></span><span></span></div> Thinking...
            </div>
          </div>
        )}
        {messages.length <= 1 && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-sm)', marginTop: 'var(--space-md)' }}>
            {SUGGESTIONS.map(suggestion => (
              <button key={suggestion} className="btn btn-ghost" style={{ padding: '8px 14px', fontSize: '0.85rem', border: '1px solid var(--border)', borderRadius: 'var(--radius-lg)' }} onClick={() => { setInput(suggestion); setTimeout(() => inputRef.current?.focus(), 50); }}>
                {suggestion}
              </button>
            ))}
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      <div style={{ flexShrink: 0, display: 'flex', gap: 'var(--space-sm)', padding: 'var(--space-md) 0', borderTop: '1px solid var(--border)', alignItems: 'center' }}>
        <input type="file" ref={fileInputRef} style={{ display: 'none' }} accept="application/pdf,image/*" onChange={handleFileUpload} />
        <button className="btn btn-ghost" style={{ padding: '8px', fontSize: '1.2rem', color: 'var(--text-secondary)' }} onClick={() => fileInputRef.current?.click()} title="Upload Document">
          📎
        </button>
        <button className={`btn btn-ghost ${recording ? 'recording-pulse' : ''}`} style={{ padding: '8px', fontSize: '1.2rem', color: recording ? 'var(--critical)' : 'var(--text-secondary)' }} onClick={toggleRecording} title="Voice Input">
          🎤
        </button>
        <input ref={inputRef} type="text" className="form-input" placeholder="Type a message or reminder..." value={input} onChange={e => setInput(e.target.value)} onKeyDown={handleKeyDown} disabled={loading} style={{ flex: 1 }} autoFocus />
        <button className="btn btn-primary" onClick={() => handleSend()} disabled={loading || !input.trim()} style={{ flexShrink: 0 }}>
          {loading ? '...' : 'Send'}
        </button>
      </div>
    </div>
  );
};
