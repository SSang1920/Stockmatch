import axios from 'axios'

 const instance = axios.create({
    baseURL: '/api',
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
        }
});

instance.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken');

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
        },
    (error) => {
        return Promise.reject(error);
        }
    );

instance.interceptors.response.use(
    (response) => {
        return response},
        async (error) => {
            if (error.response && error.response.status === 401) {
                console.log('토큰이 만료되었습니다.');
                }

            return Promise.reject(error);
            }
        );

export default instance;