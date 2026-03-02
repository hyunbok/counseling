import axios from 'axios';

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
    'X-Tenant-Id': process.env.NEXT_PUBLIC_TENANT_ID ?? 'default',
  },
});

export default api;
