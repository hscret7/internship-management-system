package com.internship.service;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class Sprint3WorkflowValidationTest {
    @Test void weekBoundaries(){var start=LocalDate.of(2026,9,1);Sprint3WorkflowService.validateWeek(1,start,start);Sprint3WorkflowService.validateWeek(2,start,start.plusDays(7));assertThrows(IllegalArgumentException.class,()->Sprint3WorkflowService.validateWeek(2,start,start.plusDays(6)));assertThrows(IllegalArgumentException.class,()->Sprint3WorkflowService.validateWeek(0,start,start.plusDays(90)));}
    @Test void emptyRequiredText(){assertThrows(IllegalArgumentException.class,()->Sprint3WorkflowService.text(" ",100,true));assertEquals("",Sprint3WorkflowService.text(null,100,false));assertThrows(IllegalArgumentException.class,()->Sprint3WorkflowService.text("x".repeat(101),100,false));}
    @Test void scoreValidationBeforeDatabase(){var service=new Sprint3WorkflowService(()->{throw new AssertionError("Must not connect for invalid scores");},null,null);assertThrows(IllegalArgumentException.class,()->service.assess(1,2,0,5,"Reason"));assertThrows(IllegalArgumentException.class,()->service.assess(1,2,5,6,"Reason"));assertThrows(IllegalArgumentException.class,()->service.assess(1,2,5,5," "));}
    @Test void filterValidation(){assertThrows(IllegalArgumentException.class,()->new EndTermReportService.Filter(-1,null,null));assertThrows(IllegalArgumentException.class,()->new EndTermReportService.Filter(null,null,"UNKNOWN"));}
}
