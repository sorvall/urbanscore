import { Component } from 'react';

export class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  render() {
    if (this.state.error) {
      return (
        <div
          style={{
            minHeight: '100vh',
            padding: '24px',
            fontFamily: 'system-ui, sans-serif',
            background: '#f8fafc',
            color: '#1e293b'
          }}
        >
          <h1 style={{ fontSize: '1.25rem', marginBottom: '12px' }}>Ошибка при загрузке интерфейса</h1>
          <pre
            style={{
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              background: '#fff',
              padding: '16px',
              borderRadius: '8px',
              border: '1px solid #e2e8f0',
              fontSize: '13px'
            }}
          >
            {this.state.error?.message || String(this.state.error)}
          </pre>
          <p style={{ marginTop: '16px', fontSize: '14px', color: '#64748b' }}>
            Обновите страницу. Если не помогает — откройте консоль разработчика (F12 → Console) и проверьте вкладку Network: загружаются ли файлы из /assets/.
          </p>
        </div>
      );
    }
    return this.props.children;
  }
}
