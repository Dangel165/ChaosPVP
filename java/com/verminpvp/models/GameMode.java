package com.verminpvp.models;

/**
 * Game mode enum for team vs solo play
 */
public enum GameMode {
    TEAM("팀전"),
    SOLO("개인전");
    
    private final String displayName;
    
    GameMode(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
