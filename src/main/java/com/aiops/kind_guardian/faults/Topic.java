package com.aiops.kind_guardian.faults;

public enum Topic {
    APP_DESIGN(ExamType.CKAD),
    WORKLOADS(ExamType.CKAD),
    SERVICES_NETWORKING(ExamType.CKAD),
    CONFIGURATION(ExamType.CKAD),
    OBSERVABILITY(ExamType.CKAD),

    CLUSTER_ARCHITECTURE(ExamType.CKA),
    STORAGE(ExamType.CKA),
    TROUBLESHOOTING(ExamType.CKA),

    CLUSTER_HARDENING(ExamType.CKS),
    SUPPLY_CHAIN_SECURITY(ExamType.CKS),
    RUNTIME_SECURITY(ExamType.CKS);

    public final ExamType exam;
    Topic(ExamType exam) { this.exam = exam; }
}