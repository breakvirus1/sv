import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import { queryKeys } from './queryKeys';

export const useCompanies = (options = {}) => {
  return useQuery({
    queryKey: queryKeys.companies('list'),
    queryFn: async () => {
      const r = await api.get('/api/v1/companies?size=100');
      return r.data.content || [];
    },
    ...options,
  });
};

export const useEmployees = (params = {}) => {
  const size = params.size || 100;
  const q = params.q || '';
  const companyId = params.companyId;
  return useQuery({
    queryKey: queryKeys.employees(q ? { q, size, companyId } : { size, companyId }),
    queryFn: async () => {
      const companyParam = companyId ? `&companyId=${companyId}` : '';
      const r = await api.get(`/api/v1/employees?size=${size}${q ? `&q=${q}` : ''}${companyParam}`);
      return r.data.content || [];
    },
    enabled: !!params.enabled,
  });
};

export const useEmployeeByUsername = (username) => {
  return useQuery({
    queryKey: queryKeys.employeeByUsername(username),
    queryFn: async () => {
      const r = await api.get(`/api/v1/employees?size=1&q=${username}`);
      return (r.data.content || [])[0] || null;
    },
    enabled: !!username,
  });
};

export const useClients = () => {
  return useQuery({
    queryKey: queryKeys.clients('list'),
    queryFn: async () => {
      const r = await api.get('/api/v1/clients?size=100');
      return r.data.content || [];
    },
  });
};

export const useMaterials = (options = {}) => {
  return useQuery({
    queryKey: queryKeys.materials('list'),
    queryFn: async () => {
      const r = await api.get('/api/v1/materials?size=100');
      return r.data.content || [];
    },
    ...options,
  });
};

export const useOperations = (options = {}) => {
  return useQuery({
    queryKey: queryKeys.operations,
    queryFn: async () => {
      const r = await api.get('/api/v1/calculations/operations');
      return r.data || [];
    },
    ...options,
  });
};

export const useGroupedOperations = (materialId = null) => {
  return useQuery({
    queryKey: queryKeys.groupedOperations(materialId),
    queryFn: async () => {
      const url = materialId
        ? `/api/v1/calculations/operations/grouped?materialId=${materialId}`
        : '/api/v1/calculations/operations/grouped';
      const r = await api.get(url);
      return r.data || { groups: {}, ungroupedOperations: [] };
    },
    enabled: materialId !== undefined,
  });
};

export const useEyelets = () => {
  return useQuery({
    queryKey: queryKeys.eyelets,
    queryFn: async () => {
      const r = await api.get('/api/v1/calculations/eyelets');
      return r.data || [];
    },
  });
};

export const useNotifications = () => {
  return useQuery({
    queryKey: queryKeys.notifications,
    queryFn: async () => {
      const r = await api.get('/api/v1/notifications');
      return r.data || [];
    },
  });
};

export const useUnreadNotifications = () => {
  return useQuery({
    queryKey: queryKeys.unreadNotifications,
    queryFn: async () => {
      const r = await api.get('/api/v1/notifications/unread-count');
      return r.data.count || 0;
    },
  });
};

export const useWorkshops = () => {
  return useQuery({
    queryKey: queryKeys.workshops,
    queryFn: async () => {
      const r = await api.get('/api/v1/workshops?size=100');
      return r.data.content || [];
    },
  });
};

export const useProducts = () => {
  return useQuery({
    queryKey: queryKeys.products,
    queryFn: async () => {
      const r = await api.get('/api/v1/products');
      return r.data || [];
    },
  });
};

export const useOperationGroups = () => {
  return useQuery({
    queryKey: queryKeys.operationGroups,
    queryFn: async () => {
      const r = await api.get('/api/v1/admin/operation-groups');
      return r.data || [];
    },
  });
};
