package com.arequiparescue.reporting.presentation;

import com.arequiparescue.reporting.domain.ExperimentalEmergencyRecord;
import com.arequiparescue.reporting.domain.ReportingSourceData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportingCsvExporterTest {

    @Test
    void exportsUtf8BomAndEighteenColumnHeaderWhenEmpty() {
        byte[] bytes = ReportingCsvExporter.exportTraceabilityCsv(List.of());

        assertTrue(bytes.length >= 3);
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertEquals(
                "\uFEFFemergency_id,state,priority,category,rule_version_id,assigned_ambulance_id,"
                        + "hospital_id,last_outcome_reason,registered_at_t,queue_entered_at_t,"
                        + "assigned_at_t,at_incident_at_t,hospital_queue_entered_at_t,"
                        + "in_transport_at_t,completed_at_t,discharged_at_t,"
                        + "final_operations_count,attempt_logs_count\r\n",
                csv
        );
    }

    @Test
    void escapesRfc4180SpecialCharactersAndNeutralizesFormulaInjection() {
        ExperimentalEmergencyRecord record = new ExperimentalEmergencyRecord(
                "EMG-00001",
                "COMPLETED",
                "RUL-V01",
                "AMB-01",
                "HOSP-01",
                "=CMD|' /C calc'!A0, \"nota\"",
                1L, 1L, 2L, 4L, null, 4L, 8L, 12L,
                List.of(new ExperimentalEmergencyRecord.OperationResultTrace(
                        "OP-1", "AUTOMATIC_DISPATCH", "AMB-01", 1, "ASSIGNED", "OK", 2L)),
                List.of(
                        new ExperimentalEmergencyRecord.AttemptLogTrace(
                                "OP-1", "AUTOMATIC_DISPATCH", "AMB-01", 1, "ASSIGNED", "OK", 2L),
                        new ExperimentalEmergencyRecord.AttemptLogTrace(
                                "OP-2", "HOSPITAL_RESERVATION", "HOSP-01", 1, "RESERVED", "OK", 4L)
                ),
                "HIGH",
                "INC-A"
        );

        String csv = new String(ReportingCsvExporter.exportTraceabilityCsv(List.of(record)), StandardCharsets.UTF_8);
        String[] lines = csv.split("\r\n");
        assertEquals(2, lines.length);
        assertTrue(lines[1].startsWith("EMG-00001,COMPLETED,HIGH,INC-A,RUL-V01,AMB-01,HOSP-01,"));
        assertTrue(lines[1].contains("\"'=CMD|' /C calc'!A0, \"\"nota\"\"\""));
        assertTrue(lines[1].endsWith(",1,1,2,4,,4,8,12,1,2"));
    }

    @Test
    void neutralizesAllDangerousFormulaPrefixesIncludingLeadingWhitespaceAndControlChars() {
        assertEquals("\"'=SUM(A1:A2)\"", ReportingCsvExporter.formatCell("=SUM(A1:A2)"));
        assertEquals("\"'+1+1\"", ReportingCsvExporter.formatCell("+1+1"));
        assertEquals("\"'-1-1\"", ReportingCsvExporter.formatCell("-1-1"));
        assertEquals("\"'@SUM(A1)\"", ReportingCsvExporter.formatCell("@SUM(A1)"));
        assertEquals("\"'\t=1+1\"", ReportingCsvExporter.formatCell("\t=1+1"));
        assertEquals("\"'  =2+2\"", ReportingCsvExporter.formatCell("  =2+2"));
        assertEquals("\"'\r\n+cmd\"", ReportingCsvExporter.formatCell("\r\n+cmd"));
        assertEquals("NORMAL_VALUE", ReportingCsvExporter.formatCell("NORMAL_VALUE"));
        assertEquals("\"con, coma\"", ReportingCsvExporter.formatCell("con, coma"));
        assertEquals("", ReportingCsvExporter.formatCell(null));
    }
}
