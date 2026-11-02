package com.example.printsv.common.tenant;

/**
 * Хранит идентификатор текущей компании в контексте потока.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    /**
     * Выполняет действие под указанным тенантом, восстанавливая предыдущий контекст.
     */
    public static void runAs(Long tenantId, Runnable action) {
        Long previous = CURRENT_TENANT.get();
        try {
            CURRENT_TENANT.set(tenantId);
            action.run();
        } finally {
            if (previous == null) {
                CURRENT_TENANT.remove();
            } else {
                CURRENT_TENANT.set(previous);
            }
        }
    }
}
