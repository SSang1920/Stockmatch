import { useEffect, useState } from 'react'
import { api } from '../lib/api'
export default function Dashboard() {
    const [status, setStatus] = useState('loading')
    useEffect(() => {
        api.get('/health')
            .then(r => setStatus(r.data.status))
            .catch(() => setStatus('error')) 
    }, [])
    
    return <div>백엔드 상태: {status}</div>
}