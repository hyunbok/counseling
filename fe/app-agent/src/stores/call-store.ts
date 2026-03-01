import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

type ActiveTab = 'chat' | 'note' | 'file' | 'doc';

interface CallState {
  channelId: string | null;
  customerName: string | null;
  activeTab: ActiveTab;
  notesDraft: string;
  setChannel: (channelId: string, customerName: string) => void;
  setActiveTab: (tab: ActiveTab) => void;
  setNotesDraft: (draft: string) => void;
  reset: () => void;
}

const useCallStore = create<CallState>()(
  persist(
    (set) => ({
      channelId: null,
      customerName: null,
      activeTab: 'chat',
      notesDraft: '',
      setChannel: (channelId, customerName) => set({ channelId, customerName }),
      setActiveTab: (tab) => set({ activeTab: tab }),
      setNotesDraft: (draft) => set({ notesDraft: draft }),
      reset: () =>
        set({
          channelId: null,
          customerName: null,
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
