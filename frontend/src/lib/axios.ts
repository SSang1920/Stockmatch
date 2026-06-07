import axios from 'axios'

const instance = axios.create({
    baseURL: '/api',
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
        }
});

export const publicInstance = axios.create({
    baseURL: '/api',
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
        }
});

let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any) => {
    failedQueue.forEach((prom) => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve();
        }
    });

    failedQueue = [];
};

instance.interceptors.request.use(
    (config) => {
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
                        resolve: () => {
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
                    withCredentials: true
                });

                console.log('재발급 성공! AccessToken 갱신.');

                processQueue(null);


                return instance(originalRequest);

                } catch (refreshError) {
                    // 재발급 요청조차 실패하면 진짜 로그인 풀린 것임
                    console.error('재발급 실패 (쿠키 만료됨). 로그아웃 처리.');
                    processQueue(refreshError, null);

                    localStorage.removeItem('accessToken');
                    sessionStorage.clear();

                    window.location.href = '/sign-in';
                    return Promise.reject(refreshError);
                }finally {
                     isRefreshing = false;
                 }
            }
        return Promise.reject(error);
    }
);

export default instance;