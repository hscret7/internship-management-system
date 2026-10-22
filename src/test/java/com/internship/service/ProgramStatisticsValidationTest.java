package com.internship.service;
import com.internship.dao.ProgramStatisticsDAO;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class ProgramStatisticsValidationTest {
    @Test void emptyDenominatorAndRounding() { assertEquals("0.00",ProgramStatisticsDAO.rate(0,0).toString());assertEquals("33.33",ProgramStatisticsDAO.rate(1,3).toString()); }
    @Test void invalidFilters() {
        assertThrows(IllegalArgumentException.class,()->new ProgramStatisticsDAO.Filter(0,null,null,null));
        assertThrows(IllegalArgumentException.class,()->new ProgramStatisticsDAO.Filter(null,"UNKNOWN",null,null));
        assertThrows(IllegalArgumentException.class,()->new ProgramStatisticsDAO.Filter(null,null,LocalDate.now(),null));
    }
}
