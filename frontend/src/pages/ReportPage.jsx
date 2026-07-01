import React, { useState } from 'react';
import apiClient from '../api/axiosClient';

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}
function daysAgoIso(n) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
}

export default function ReportPage() {
  const [startDate, setStartDate] = useState(daysAgoIso(7));
  const [endDate, setEndDate] = useState(todayIso());
  const [report, setReport] = useState(null);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const fetchReport = async (targetPage = 0) => {
    setError('');
    setLoading(true);
    try {
      const { data } = await apiClient.get('/api/reports/verifications', {
        params: { startDate, endDate, page: targetPage, size: 50 }
      });
      setReport(data);
      setPage(targetPage);
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load report.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    fetchReport(0);
  };

  return (
    <div>
      <h1 className="page-title">Verification Reports</h1>
      <p className="page-sub">Track every floor-verification event within a date range.</p>

      <div className="card">
        {error && <div className="error-banner">{error}</div>}
        <form onSubmit={handleSubmit} className="filter-row">
          <div className="field">
            <label htmlFor="startDate">Start Date</label>
            <input id="startDate" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="endDate">End Date</label>
            <input id="endDate" type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} required />
          </div>
          <button type="submit" className="btn" disabled={loading}>
            {loading ? 'Loading…' : 'Generate Report'}
          </button>
        </form>
      </div>

      {report && (
        <div className="card">
          <div className="stat-row" style={{ marginTop: 0, marginBottom: 16 }}>
            <div className="stat">
              <div className="stat-value">{report.totalElements.toLocaleString()}</div>
              <div className="stat-label">Total Verifications</div>
            </div>
          </div>

          {report.rows.length === 0 ? (
            <div className="empty-state">No verification events in this date range.</div>
          ) : (
            <>
              <table>
                <thead>
                  <tr>
                    <th>WID</th>
                    <th>EAN</th>
                    <th>Mfg Date</th>
                    <th>Expiry Date</th>
                    <th>Status</th>
                    <th>Operator</th>
                    <th>Verified At</th>
                  </tr>
                </thead>
                <tbody>
                  {report.rows.map((row) => (
                    <tr key={row.id}>
                      <td>{row.wid}</td>
                      <td>{row.ean || '—'}</td>
                      <td>{row.manufacturingDate || '—'}</td>
                      <td>{row.expiryDate || '—'}</td>
                      <td>
                        <span className={'tag ' + (row.found ? 'tag-success' : 'tag-danger')}>
                          {row.found ? 'Found' : 'Not Found'}
                        </span>
                      </td>
                      <td style={{ fontFamily: 'var(--font-body)' }}>{row.operatorUsername}</td>
                      <td>{row.verifiedAt?.replace('T', ' ').slice(0, 19)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <div className="pagination">
                <button
                  className="btn btn-secondary"
                  disabled={page === 0 || loading}
                  onClick={() => fetchReport(page - 1)}
                >
                  Previous
                </button>
                <span>Page {page + 1} of {Math.max(report.totalPages, 1)}</span>
                <button
                  className="btn btn-secondary"
                  disabled={page + 1 >= report.totalPages || loading}
                  onClick={() => fetchReport(page + 1)}
                >
                  Next
                </button>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
