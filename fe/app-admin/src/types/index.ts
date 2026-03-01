export type AdminRole = 'SUPER_ADMIN' | 'COMPANY_ADMIN' | 'GROUP_ADMIN';

export interface Tenant {
  id: string;
  name: string;
  domain: string;
  plan: string;
  agentCount: number;
  createdAt: string;
}

export interface Group {
  id: string;
  tenantId: string;
  tenantName?: string;
  name: string;
  agentCount: number;
  createdAt: string;
}

export interface Agent {
  id: string;
  tenantId: string;
  groupId?: string;
  groupName?: string;
  username: string;
  name: string;
  email: string;
  status: 'ACTIVE' | 'INACTIVE' | 'BUSY';
  createdAt: string;
}

export interface MonitoringSession {
  id: string;
  agentId: string;
  agentName: string;
  clientName: string;
  status: 'WAITING' | 'IN_PROGRESS' | 'ENDED';
  startedAt: string;
  duration?: number;
}

export interface Feedback {
  id: string;
  sessionId: string;
  agentId: string;
  agentName: string;
  clientName: string;
  rating: number;
  comment?: string;
  createdAt: string;
}
