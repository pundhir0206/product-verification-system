import { createSlice } from '@reduxjs/toolkit';

const persisted = JSON.parse(localStorage.getItem('pvs_auth') || 'null');

const initialState = persisted || {
  token: null,
  username: null,
  role: null
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    loginSuccess: (state, action) => {
      const { token, username, role } = action.payload;
      state.token = token;
      state.username = username;
      state.role = role;
      localStorage.setItem('pvs_auth', JSON.stringify({ token, username, role }));
    },
    logout: (state) => {
      state.token = null;
      state.username = null;
      state.role = null;
      localStorage.removeItem('pvs_auth');
    }
  }
});

export const { loginSuccess, logout } = authSlice.actions;
export default authSlice.reducer;
