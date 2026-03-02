export type AdminRole = 'SUPER_ADMIN' | 'COMPANY_ADMIN' | 'GROUP_ADMIN';

// Matches BE TenantSummaryResponse
export interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

// Matches BE GroupResponse
export interface Group {
  id: string;
  name: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

// Matches BE AgentResponse
export interface Agent {
  id: string;
  username: string;
  name: string;
  role: string;
  groupId?: string;
  active: boolean;
  agentStatus: string;
  createdAt: string;
  updatedAt: string;
}

// Matches BE CreateAgentResponse
export interface CreateAgentResult {
  id: string;
  username: string;
  name: string;
  role: string;
  groupId?: string;
  temporaryPassword: string;
  active: boolean;
  createdAt: string;
}

// Matches BE ActiveChannelResponse
export interface ActiveChannel {
  id: string;
  agentId?: string;
  status: string;
  startedAt?: string;
  createdAt: string;
}

// Matches BE AgentStatusInfo
export interface AgentStatusInfo {
  agentId: string;
  agentName: string;
  status: string;
  active: boolean;
}

// Matches BE FeedbackResponse
export interface Feedback {
  id: string;
  channelId: string;
  rating: number;
  comment?: string;
  createdAt: string;
}

// Matches BE StatsSummaryResponse
export interface StatsSummary {
  totalChannels: number;
  completedChannels: number;
  averageRating: number;
  averageHandleTimeSeconds: number;
}

// Matches BE AgentStatsResponse
export interface AgentStats {
  agentId: string;
  agentName: string;
  totalChannels: number;
  completedChannels: number;
  averageRating: number;
  averageHandleTimeSeconds: number;
}
