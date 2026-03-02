import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

type ActiveTab = 'chat' | 'note' | 'file' | 'doc';

interface CallState {
  channelId: string | null;
  customerName: string | null;
  agentToken: string | null;
  livekitUrl: string | null;
  activeTab: ActiveTab;
  notesDraft: string;
  setChannel: (channelId: string, customerName: string, agentToken?: string, livekitUrl?: string) => void;
  setActiveTab: (tab: ActiveTab) => void;
  setNotesDraft: (draft: string) => void;
  reset: () => void;
}

const useCallStore = create<CallState>()(
  persist(
    (set) => ({
      channelId: null,
      customerName: null,
      agentToken: null,
      livekitUrl: null,
      activeTab: 'chat',
      notesDraft: '',
      setChannel: (channelId, customerName, agentToken, livekitUrl) =>
        set({ channelId, customerName, agentToken: agentToken ?? null, livekitUrl: livekitUrl ?? null }),
      setActiveTab: (tab) => set({ activeTab: tab }),
      setNotesDraft: (draft) => set({ notesDraft: draft }),
      reset: () =>
        set({
          channelId: null,
          customerName: null,
          agentToken: null,
          livekitUrl: null,
          activeTab: 'chat',
          notesDraft: '',
        }),
    }),
    {
      name: 'call-store',
      storage: createJSONStorage(() => sessionStorage),
    },
  ),
);

export default useCallStore;
