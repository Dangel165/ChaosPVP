package com.verminpvp.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClassType enum
 */
class ClassTypeTest {
    
    @Test
    void testClassTypeCount() {
        assertEquals(7, ClassType.values().length, "Should have exactly 7 classes");
    }
    
    @Test
    void testFromStringWithEnumName() {
        assertEquals(ClassType.SWORDSMAN, ClassType.fromString("SWORDSMAN"));
        assertEquals(ClassType.SCIENTIST, ClassType.fromString("scientist"));
        assertEquals(ClassType.CAPTAIN, ClassType.fromString("CaPtAiN"));
    }
    
    @Test
    void testFromStringWithDisplayName() {
        assertEquals(ClassType.SWORDSMAN, ClassType.fromString("Swordsman"));
        assertEquals(ClassType.PLAGUE_SPREADER, ClassType.fromString("Plague Spreader"));
        assertEquals(ClassType.SHIELD_SOLDIER, ClassType.fromString("shield soldier"));
    }
    
    @Test
    void testFromStringWithKoreanName() {
        assertEquals(ClassType.SWORDSMAN, ClassType.fromString("검사"));
        assertEquals(ClassType.SCIENTIST, ClassType.fromString("과학자"));
        assertEquals(ClassType.CAPTAIN, ClassType.fromString("선장"));
    }
    
    @Test
    void testFromStringInvalid() {
        assertNull(ClassType.fromString("InvalidClass"));
        assertNull(ClassType.fromString(""));
        assertNull(ClassType.fromString("123"));
    }
    
    @Test
    void testDisplayNames() {
        assertEquals("Swordsman", ClassType.SWORDSMAN.getDisplayName());
        assertEquals("Scientist", ClassType.SCIENTIST.getDisplayName());
        assertEquals("Plague Spreader", ClassType.PLAGUE_SPREADER.getDisplayName());
        assertEquals("Shield Soldier", ClassType.SHIELD_SOLDIER.getDisplayName());
        assertEquals("Critical Cutter", ClassType.CRITICAL_CUTTER.getDisplayName());
        assertEquals("Navigator", ClassType.NAVIGATOR.getDisplayName());
        assertEquals("Captain", ClassType.CAPTAIN.getDisplayName());
    }
    
    @Test
    void testKoreanNames() {
        assertEquals("검사", ClassType.SWORDSMAN.getKoreanName());
        assertEquals("과학자", ClassType.SCIENTIST.getKoreanName());
        assertEquals("역병 살포자", ClassType.PLAGUE_SPREADER.getKoreanName());
        assertEquals("방패 병사", ClassType.SHIELD_SOLDIER.getKoreanName());
        assertEquals("크리티컬 커터", ClassType.CRITICAL_CUTTER.getKoreanName());
        assertEquals("항해사", ClassType.NAVIGATOR.getKoreanName());
        assertEquals("선장", ClassType.CAPTAIN.getKoreanName());
    }
}
