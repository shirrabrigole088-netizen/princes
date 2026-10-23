package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class AttendanceRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String recordId;
    private String studentNo;
    private LocalDate date;
    private LocalTime timeIn;          // null when Absent / Excused
    private AttendanceStatus status;
    private String remarks;

    public AttendanceRecord(String studentNo, LocalDate date, LocalTime timeIn,
                            AttendanceStatus status, String remarks) {
        // FIX: the sample's second Item constructor overwrote the id it was given.
        this(UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                studentNo, date, timeIn, status, remarks);
    }

    public AttendanceRecord(String recordId, String studentNo, LocalDate date, LocalTime timeIn,
                            AttendanceStatus status, String remarks) {
        this.recordId = recordId;
        this.studentNo = studentNo;
        this.date = date;
        this.timeIn = timeIn;
        this.status = status;
        this.remarks = remarks;
    }

    public String getRecordId() { return recordId; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getTimeIn() { return timeIn; }
    public void setTimeIn(LocalTime timeIn) { this.timeIn = timeIn; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    @Override
    public String toString() {
        return "AttendanceRecord{" + recordId + ", " + studentNo + ", " + date + ", "
                + timeIn + ", " + status + "}";
    }
}
