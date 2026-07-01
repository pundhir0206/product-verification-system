import React, { useEffect, useState } from 'react';
import apiClient from '../api/axiosClient';

export default function UserManagementPage() {
  const [users, setUsers] = useState([]);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('OPERATOR');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const loadUsers = async () => {
    try {
      const { data } = await apiClient.get('/api/users');
      setUsers(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load users.');
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleCreate = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);
    try {
      await apiClient.post('/api/users', { username, password, role });
      setSuccess(`User "${username}" created.`);
      setUsername('');
      setPassword('');
      setRole('OPERATOR');
      loadUsers();
    } catch (err) {
      setError(err.response?.data?.message || 'Could not create user.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h1 className="page-title">User Management</h1>
      <p className="page-sub">Create warehouse operator and admin accounts, and assign roles.</p>

      <div className="card">
        {error && <div className="error-banner">{error}</div>}
        {success && <div className="stat-label" style={{ color: 'var(--success)', marginBottom: 12 }}>{success}</div>}
        <form onSubmit={handleCreate} className="filter-row">
          <div className="field">
            <label htmlFor="newUsername">Username</label>
            <input id="newUsername" type="text" value={username} onChange={(e) => setUsername(e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="newPassword">Password</label>
            <input id="newPassword" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="role">Role</label>
            <select
              id="role"
              value={role}
              onChange={(e) => setRole(e.target.value)}
              style={{ background: 'var(--bg)', border: '1px solid var(--border)', color: 'var(--text)', padding: '10px 12px', borderRadius: 6, fontSize: 14 }}
            >
              <option value="OPERATOR">Operator</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>
          <button type="submit" className="btn" disabled={loading}>
            {loading ? 'Creating…' : 'Create User'}
          </button>
        </form>
      </div>

      <div className="card">
        <table>
          <thead>
            <tr>
              <th>Username</th>
              <th>Role</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td style={{ fontFamily: 'var(--font-body)' }}>{u.username}</td>
                <td>
                  <span className="tag tag-success">{u.role}</span>
                </td>
                <td style={{ fontFamily: 'var(--font-body)' }}>{u.enabled ? 'Active' : 'Disabled'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
