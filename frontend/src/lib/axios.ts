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
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // 401 에러 감지
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                    console.log( '401 감지! 쿠키를 믿고 재발급 요청 시도...');

                    const res = await axios.post('http://localhost:8080/api/auth/refresh', {}, {
                        withCredentials: true //
                    });

                    console.log('재발급 성공! AccessToken 갱신.');

                    const newAccessToken = res.data.data?.accessToken || res.data.accessToken;

                    if (newAccessToken) {
                        // 새 토큰 저장
                        localStorage.setItem('accessToken', newAccessToken);

                        // 실패했던 요청에 새 토큰 끼워서 재요청
                        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                        return axios(originalRequest);
                    }
                } catch (refreshError) {
                    // 재발급 요청조차 실패하면 진짜 로그인 풀린 것임
                    console.error('재발급 실패 (쿠키 만료됨). 로그아웃 처리.');
                    localStorage.removeItem('accessToken');
                    return Promise.reject(refreshError);
                }
            }
        return Promise.reject(error);
    }
);

export default instance;