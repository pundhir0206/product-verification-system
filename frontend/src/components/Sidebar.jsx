import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { logout } from '../store/authSlice';

export default function Sidebar() {
  const { username, role } = useSelector((state) => state.auth);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  return (
    <aside className="sidebar">
      <div className="brand">PRODUCT VERIFICATION</div>
      <div className="brand-sub">WAREHOUSE OPS · v1.0</div>
      <div className="barcode-rule" style={{ marginBottom: 20 }} />

      <nav>
        {role === 'ADMIN' && (
          <NavLink to="/upload" className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}>
            Bulk Upload
          </NavLink>
        )}
        <NavLink to="/validate" className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}>
          Floor Validation
        </NavLink>
        <NavLink to="/reports" className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}>
          Verification Reports
        </NavLink>
        {role === 'ADMIN' && (
          <NavLink to="/users" className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}>
            User Management
          </NavLink>
        )}
      </nav>

      <div className="sidebar-footer">
        <div className="user-chip">
          <strong>{username}</strong>
          <span className="role-tag">{role}</span>
        </div>
        <button className="logout-btn" onClick={handleLogout}>Log out</button>
      </div>
    </aside>
  );
}
