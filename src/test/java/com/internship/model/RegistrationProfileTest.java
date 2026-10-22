package com.internship.model;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class RegistrationProfileTest {
    @Test void unicodeFieldsAndGpaArePreserved() {
        var p=RegistrationProfile.parse(" +84 912345678 "," ICTU "," An toàn thông tin ","3.75");
        assertEquals("ICTU",p.university());assertEquals("An toàn thông tin",p.major());
        assertEquals("+84 912345678",p.phone());assertEquals(new BigDecimal("3.75"),p.gpa());
    }
    @Test void optionalFieldsHaveExplicitEmptyAndNullValues() {
        var p=RegistrationProfile.parse(null," ",null,"");
        assertEquals("",p.phone());assertEquals("",p.university());assertEquals("",p.major());assertNull(p.gpa());
    }
    @Test void boundariesAreAccepted() {
        for(String g:new String[]{"0","0.00","4","4.00"}) assertNotNull(RegistrationProfile.parse(null,null,null,g));
        assertEquals(150,RegistrationProfile.parse(null,"a".repeat(150),"b".repeat(100),null).university().length());
    }
    @Test void invalidGpaIsRejectedWithoutRoundingOrSpecialValues() {
        for(String g:new String[]{"-1","4.01","3.999","NaN","Infinity","1e2","1,5","999999999999999999999"})
            assertThrows(IllegalArgumentException.class,()->RegistrationProfile.parse(null,null,null,g),g);
    }
    @Test void lengthsControlsAndBadPhoneAreRejected() {
        assertThrows(IllegalArgumentException.class,()->RegistrationProfile.parse(null,"a".repeat(151),null,null));
        assertThrows(IllegalArgumentException.class,()->RegistrationProfile.parse(null,null,"a".repeat(101),null));
        assertThrows(IllegalArgumentException.class,()->RegistrationProfile.parse("091abc1234",null,null,null));
        assertThrows(IllegalArgumentException.class,()->RegistrationProfile.parse("123",null,null,null));
        assertThrows(IllegalArgumentException.class,()->RegistrationProfile.parse(null,"a\nb",null,null));
    }
}
