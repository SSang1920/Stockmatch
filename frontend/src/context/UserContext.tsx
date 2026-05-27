import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { getUserInfo } from '@/api/user';
import { cookies } from '@/lib/cookies';

export interface User {
    id: number;             // Long id -> number
    email: string;          // String email
    name: string;           // String name
    profileImageUrl?: string; // String profileImageUrl (nullable 가능성 있음)
    role: string;           // UserRole (USER, ADMIN 등)
    provider: string;       // AuthProvider (GOOGLE, NAVER 등)
    status: string;         // UserStatus (ACTIVE, DELETED)

    investmentType?: string | null;
}


interface UserContextType {
    user: User | null;
    isLoading: boolean;
    refreshUser: () => Promise<void>;
}


const UserContext = createContext<UserContextType | null>(null);

export function UserProvider({ children }: { children: React.ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    const fetchUser = useCallback(async () => {
        const token = cookies.get('accessToken');
        if (!token) {
            setUser(null);
            setIsLoading(false);
            return;
        }

        try {
            if (!user) setIsLoading(true);

            const response = await getUserInfo();

            if (response && response.data) {
                console.log("[Context] 유저 정보 로드 성공:", response.data.name);
                setUser(response.data);
            }
        } catch (error) {
            console.log("ℹ[Context] 비로그인 상태이거나 토큰 만료됨");
            setUser(null);
        } finally {
            setIsLoading(false); // 로딩 끝
        }
    }, []);

    useEffect(() => {
        fetchUser();
    }, [fetchUser]);

    return (
        <UserContext.Provider value={{ user, isLoading, refreshUser: fetchUser }}>
            {children}
        </UserContext.Provider>
    );
}

export function useUser() {
    const context = useContext(UserContext);
    if (!context) {
        throw new Error('useUser must be used within a UserProvider');
    }
    return context;
}