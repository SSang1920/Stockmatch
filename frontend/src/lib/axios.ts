import axios from 'axios'

const instance = axios.create({
    baseURL: '/api',
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
        }
});
let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
    failedQueue.forEach((prom) => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });

    failedQueue = [];
};

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
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({
                        resolve: (token: string) => {
                            originalRequest.headers['Authorization'] = `Bearer ${token}`;
                            resolve(instance(originalRequest));
                        },
                        reject: (err: any) => {
                            reject(err);
                        }
                    });
                });
            }

            originalRequest._retry = true;
            isRefreshing = true; //

            try {
                console.log( '401 감지! 쿠키를 믿고 재발급 요청 시도...');

                const res = await axios.post('/api/auth/refresh', {}, {
                    withCredentials: true //
                });

                console.log('재발급 성공! AccessToken 갱신.');

                const newAccessToken = res.data.data?.accessToken || res.data.accessToken;

                if (newAccessToken) {
                    // 새 토큰 저장
                    localStorage.setItem('accessToken', newAccessToken);

                    instance.defaults.headers.common['Authorization'] = `Bearer ${newAccessToken}`;

                    document.cookie = `accessToken=${newAccessToken}; path=/;`;

                    processQueue(null, newAccessToken);

                    // 실패했던 요청에 새 토큰 끼워서 재요청
                    originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                    return instance(originalRequest);
                    }
                } catch (refreshError) {
                    // 재발급 요청조차 실패하면 진짜 로그인 풀린 것임
                    console.error('재발급 실패 (쿠키 만료됨). 로그아웃 처리.');
                    processQueue(refreshError, null);
                    localStorage.removeItem('accessToken');
                    document.cookie = "accessToken=; Max-Age=0; path=/;";
                    return Promise.reject(refreshError);
                }finally {
                     isRefreshing = false;
                 }
            }
        return Promise.reject(error);
    }
);

export default instance;