package com.davidblackcn.buildupvitals.food.overeating;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OvereatStateTest {
    @Test void overflowAndCap() {
        assertEquals(0, OvereatState.EMPTY.eat(12, 8).load());
        assertEquals(8, OvereatState.EMPTY.eat(20, 8).load());
        assertEquals(2, OvereatState.EMPTY.eat(20, 2).load());
        assertEquals(4, OvereatState.EMPTY.eat(16, 8).load());
        assertEquals(80, OvereatState.EMPTY.eat(20, Integer.MAX_VALUE).load());
    }
    @Test void eightSteaksWarningHysteresisAndRearm() {
        var s = OvereatState.EMPTY;
        for (int i=1;i<=8;i++) {
            s=s.eat(20,8);
            assertEquals(i>=6,s.warned());assertEquals(i>=8,s.overfull());
        }
        for (int i=0;i<32*40;i++) s=s.tick();
        assertEquals(32,s.load());assertTrue(s.overfull());assertTrue(s.warned());
        for (int i=0;i<40;i++) s=s.tick();
        assertEquals(31,s.load());assertFalse(s.overfull());assertFalse(s.warned());
        assertTrue(s.eat(20,17).warned());
    }
    @Test void codecPreservesHysteresisAndPartialDigestion() {
        var s=new OvereatState(40,17,true,true);
        assertEquals(s,OvereatState.CODEC.parse(JsonOps.INSTANCE,OvereatState.CODEC.encodeStart(JsonOps.INSTANCE,s).getOrThrow()).getOrThrow());
    }
}
