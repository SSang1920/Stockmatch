import axios from '../lib/axios';

export const getUserInfo = async () => {
    const response = await axios.get('/user/me');
    return response.data;
  };

export const logoutApi = async () => {
    const response = await axios.post('/auth/logout');
    return response.data;
    };