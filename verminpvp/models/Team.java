package com.verminpvp.models;

/**
 * Represents the teams in team mode
 */
public enum Team {
    BLUE("§9블루팀"),
    RED("§c레드팀");
    
    private final String displayName;
    
    Team(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
