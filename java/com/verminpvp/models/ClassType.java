package com.verminpvp.models;

/**
 * Enum representing the 8 character classes in VerminPVP
 */
public enum ClassType {
    SWORDSMAN("Swordsman", "검사"),
    SCIENTIST("Scientist", "과학자"),
    PLAGUE_SPREADER("Plague Spreader", "역병 살포자"),
    SHIELD_SOLDIER("Shield Soldier", "방패 병사"),
    CRITICAL_CUTTER("Critical Cutter", "크리티컬 커터"),
    NAVIGATOR("Navigator", "항해사"),
    CAPTAIN("Captain", "선장"),
    SHAPESHIFTER("Shapeshifter", "생명체"),
    JUGGLER("Juggler", "저글러"),
    DRAGON_FURY("Dragon Fury", "용의 분노자"),
    UNDEAD("Undead", "언데드"),
    STAMPER("Stamper", "스탬퍼"),
    TIME_ENGRAVER("Time Engraver", "시간 각인자");
    
    private final String displayName;
    private final String koreanName;
    
    ClassType(String displayName, String koreanName) {
        this.displayName = displayName;
        this.koreanName = koreanName;
    }
    
    /**
     * Get the English display name of the class
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the Korean name of the class
     * @return The Korean name
     */
    public String getKoreanName() {
        return koreanName;
    }
    
    /**
     * Get a ClassType from a string name (case-insensitive)
     * @param name The name to parse
     * @return The ClassType, or null if not found
     */
    public static ClassType fromString(String name) {
        for (ClassType type : values()) {
            if (type.name().equalsIgnoreCase(name) || 
                type.displayName.equalsIgnoreCase(name) ||
                type.koreanName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
