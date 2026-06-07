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
                const res = await instance.post('/auth/refresh', {});

                processQueue(null);
                return instance(originalRequest);

                } catch (refreshError) {
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