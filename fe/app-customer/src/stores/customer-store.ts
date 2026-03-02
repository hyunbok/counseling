import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

interface CustomerState {
  customerName: string;
  customerContact: string;
  entryId: string | null;
  channelId: string | null;
  queuePosition: number | null;
  setCustomerInfo: (name: string, contact: string) => void;
  setEntryId: (entryId: string) => void;
  setChannelId: (channelId: string) => void;
  setQueuePosition: (position: number) => void;
  reset: () => void;
}

const useCustomerStore = create<CustomerState>()(
  persist(
    (set) => ({
      customerName: '',
      customerContact: '',
      entryId: null,
      channelId: null,
      queuePosition: null,
      setCustomerInfo: (name, contact) =>
        set({ customerName: name, customerContact: contact }),
      setEntryId: (entryId) => set({ entryId }),
      setChannelId: (channelId) => set({ channelId }),
      setQueuePosition: (position) => set({ queuePosition: position }),
      reset: () =>
        set({
          customerName: '',
          customerContact: '',
          entryId: null,
          channelId: null,
          queuePosition: null,
        }),
    }),
    {
      name: 'customer-store',
      storage: createJSONStorage(() => sessionStorage),
    },
  ),
);

export default useCustomerStore;
