import axios from 'axios'

 const instance = axios.create({
    baseURL: '/api',
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
        }
});

//추후 header에 token넣을때 사용
instance.interceptors.request.use(
    (config) => {
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