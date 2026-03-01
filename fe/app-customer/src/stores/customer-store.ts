import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

interface CustomerState {
  customerName: string;
  customerContact: string;
  channelId: string | null;
  queuePosition: number | null;
  setCustomerInfo: (name: string, contact: string) => void;
  setChannelId: (channelId: string) => void;
  setQueuePosition: (position: number) => void;
  reset: () => void;
}

const useCustomerStore = create<CustomerState>()(
  persist(
    (set) => ({
      customerName: '',
      customerContact: '',
      channelId: null,
      queuePosition: null,
      setCustomerInfo: (name, contact) =>
        set({ customerName: name, customerContact: contact }),
      setChannelId: (channelId) => set({ channelId }),
      setQueuePosition: (position) => set({ queuePosition: position }),
      reset: () =>
        set({
          customerName: '',
          customerContact: '',
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
