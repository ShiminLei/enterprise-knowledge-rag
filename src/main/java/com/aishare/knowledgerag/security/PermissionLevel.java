package com.aishare.knowledgerag.security;

public enum PermissionLevel {
    PUBLIC(0),
    INTERNAL(10),
    CONFIDENTIAL(20),
    RESTRICTED(30);

    private final int rank;

    PermissionLevel(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public boolean canAccess(PermissionLevel required) {
        return rank >= required.rank;
    }
}
