import axios from '../lib/axios';

export const getUserInfo = async () => {
    const response = await axios.get('/user/me');
    return response.data;
  };

export const logoutApi = async () => {
    const response = await axios.post('/auth/logout');
    return response.data;
    };

export const updateApiKey = async (key: string) => {

    const response = await axios.post('/user/me/api-key', { apiKey: key });
    return response.data;
    };

export const fetchDecryptedApiKey = async () => {
    const response = await axios.get('/user/me/api-key/decrypted');

    return response.data.data;
    };

export const deleteUser = async () => {
    const response = await axios.delete('/user/me');
    return response.data;
    }