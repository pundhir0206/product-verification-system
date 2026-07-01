import React, { useEffect, useRef, useState } from 'react';
import apiClient from '../api/axiosClient';

export default function ValidatePage() {
  const [wid, setWid] = useState('');
  const [image, setImage] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const widInputRef = useRef(null);

  useEffect(() => {
    widInputRef.current?.focus();
  }, []);

  const handleImageChange = (e) => {
    const f = e.target.files[0];
    setImage(f || null);
    setImagePreview(f ? URL.createObjectURL(f) : null);
  };

  const resetForNextScan = () => {
    setWid('');
    setImage(null);
    setImagePreview(null);
    widInputRef.current?.focus();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!wid.trim()) return;
    setError('');
    setLoading(true);
    setResult(null);

    const formData = new FormData();
    formData.append('wid', wid.trim());
    if (image) formData.append('image', image);

    try {
      const { data } = await apiClient.post('/api/validate', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setResult(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Validation failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h1 className="page-title">Floor Validation</h1>
      <p className="page-sub">
        Scan or type the WID from the physical item, optionally attach a photo, then compare
        the details below against the label.
      </p>

      <div className="card">
        {error && <div className="error-banner">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="wid">Warehouse ID (WID)</label>
            <input
              id="wid"
              ref={widInputRef}
              type="text"
              className="wid-input"
              value={wid}
              onChange={(e) => setWid(e.target.value)}
              placeholder="Scan barcode or type WID"
              autoComplete="off"
              required
            />
          </div>

          <div className="field">
            <label htmlFor="image">Capture Product Photo (optional)</label>
            <input
              id="image"
              type="file"
              accept="image/*"
              capture="environment"
              onChange={handleImageChange}
            />
            {imagePreview && <img src={imagePreview} alt="Captured product" className="camera-preview" />}
          </div>

          <button type="submit" className="btn" disabled={loading || !wid.trim()}>
            {loading ? 'Verifying…' : 'Verify Product'}
          </button>
          {result && (
            <button type="button" className="btn btn-secondary" style={{ marginLeft: 10 }} onClick={resetForNextScan}>
              Scan Next Item
            </button>
          )}
        </form>
      </div>

      {result && (
        <div className={'result-panel ' + (result.found ? 'found' : 'not-found')}>
          <span className={'tag ' + (result.found ? 'tag-success' : 'tag-danger')}>
            {result.found ? 'Found' : 'Not Found'}
          </span>
          <p style={{ margin: '10px 0 0 0', color: 'var(--text-muted)', fontSize: 13 }}>{result.message}</p>

          {result.found && (
            <div className="result-grid">
              <div>
                <div className="result-field-label">WID</div>
                <div className="result-field-value">{result.wid}</div>
              </div>
              <div>
                <div className="result-field-label">EAN</div>
                <div className="result-field-value">{result.ean}</div>
              </div>
              <div>
                <div className="result-field-label">Manufacturing Date</div>
                <div className="result-field-value">{result.manufacturingDate}</div>
              </div>
              <div>
                <div className="result-field-label">Expiry Date</div>
                <div className="result-field-value">{result.expiryDate}</div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
