import React, { useEffect, useRef, useState } from 'react';
import apiClient from '../api/axiosClient';

export default function UploadPage() {
  const [file, setFile] = useState(null);
  const [job, setJob] = useState(null);
  const [error, setError] = useState('');
  const [uploading, setUploading] = useState(false);
  const pollRef = useRef(null);

  useEffect(() => {
    return () => clearInterval(pollRef.current);
  }, []);

  const pollStatus = (jobId) => {
    pollRef.current = setInterval(async () => {
      try {
        const { data } = await apiClient.get(`/api/upload/status/${jobId}`);
        setJob(data);
        if (data.status === 'COMPLETED' || data.status === 'FAILED') {
          clearInterval(pollRef.current);
        }
      } catch (err) {
        clearInterval(pollRef.current);
        setError('Lost connection while checking job progress.');
      }
    }, 1500);
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!file) return;
    setError('');
    setUploading(true);
    setJob(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const { data } = await apiClient.post('/api/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setJob({ ...data, totalRows: 0, processedRows: 0, insertedRows: 0, updatedRows: 0, failedRows: 0 });
      pollStatus(data.jobId);
    } catch (err) {
      setError(err.response?.data?.message || 'Upload failed.');
    } finally {
      setUploading(false);
    }
  };

  const pct = job && job.totalRows > 0
    ? Math.min(100, Math.round((job.processedRows / job.totalRows) * 100))
    : (job && job.status === 'PROCESSING' ? 5 : 0);

  return (
    <div>
      <h1 className="page-title">Bulk Product Upload</h1>
      <p className="page-sub">
        Upload a CSV to populate or refresh inventory. Columns required: WID, EAN, Manufacturing_Date, Expiry_Date.
        Files with millions of rows are processed in the background — this page will keep you posted.
      </p>

      <div className="card">
        {error && <div className="error-banner">{error}</div>}
        <form onSubmit={handleUpload}>
          <div className="field">
            <label htmlFor="csvFile">CSV File</label>
            <input
              id="csvFile"
              type="file"
              accept=".csv"
              onChange={(e) => setFile(e.target.files[0])}
              required
            />
          </div>
          <button type="submit" className="btn" disabled={uploading || !file}>
            {uploading ? 'Uploading…' : 'Upload & Process'}
          </button>
        </form>
      </div>

      {job && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
            <strong style={{ fontFamily: 'var(--font-mono)', fontSize: 13 }}>
              Job {job.jobId ? job.jobId.slice(0, 8) : ''}…
            </strong>
            <span className={'tag ' + (job.status === 'FAILED' ? 'tag-danger' : 'tag-success')}>
              {job.status}
            </span>
          </div>

          {(job.status === 'PROCESSING' || job.status === 'PENDING') && (
            <div className="progress-track">
              <div className="progress-fill" style={{ width: pct + '%' }} />
            </div>
          )}

          {job.status === 'FAILED' && job.errorMessage && (
            <div className="error-banner" style={{ marginTop: 12 }}>{job.errorMessage}</div>
          )}

          <div className="stat-row">
            <div className="stat">
              <div className="stat-value">{job.totalRows?.toLocaleString?.() ?? job.totalRows}</div>
              <div className="stat-label">Total Rows</div>
            </div>
            <div className="stat">
              <div className="stat-value">{job.processedRows?.toLocaleString?.() ?? job.processedRows}</div>
              <div className="stat-label">Processed</div>
            </div>
            <div className="stat">
              <div className="stat-value" style={{ color: 'var(--success)' }}>
                {job.insertedRows?.toLocaleString?.() ?? job.insertedRows}
              </div>
              <div className="stat-label">Inserted</div>
            </div>
            <div className="stat">
              <div className="stat-value" style={{ color: 'var(--accent)' }}>
                {job.updatedRows?.toLocaleString?.() ?? job.updatedRows}
              </div>
              <div className="stat-label">Updated</div>
            </div>
            <div className="stat">
              <div className="stat-value" style={{ color: 'var(--danger)' }}>
                {job.failedRows?.toLocaleString?.() ?? job.failedRows}
              </div>
              <div className="stat-label">Failed Rows</div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
