package controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import model.AttendanceManager;
import model.AttendanceRecord;
import model.AttendanceStatus;
import model.StudentManager;

public class AttendanceController extends ChangeNotifier {
    private final AttendanceManager model;
    private final StudentManager studentModel;

    public AttendanceController(AttendanceManager model, StudentManager studentModel) {
        this.model = model;
        this.studentModel = studentModel;
    }

    /** Shared validation for add + update. Returns an error message or null. */
    private String validate(String studentNo, LocalDate date, LocalTime timeIn,
                            AttendanceStatus status, String exceptId) {
        if (studentNo == null || studentModel.findStudent(studentNo) == null)
            return "Please select a student.";
        if (date == null) return "Date is required.";
        if (date.isAfter(LocalDate.now())) return "Date cannot be in the future.";
        if (status == null) return "Please select a status.";
        if (status.needsTimeIn() && timeIn == null)
            return "Time in is required for Present / Late.";
        if (model.existsFor(studentNo, date, exceptId))
            return "This student already has an attendance record on " + date + ".";
        return null;
    }

    public String handleAddRecord(String studentNo, LocalDate date, LocalTime timeIn,
                                  AttendanceStatus status, String remarks) {
        String err = validate(studentNo, date, timeIn, status, null);
        if (err != null) return err;
        if (!status.needsTimeIn()) timeIn = null;
        AttendanceRecord r = new AttendanceRecord(studentNo, date, timeIn, status,
                remarks == null ? "" : remarks.trim());
        model.addRecord(r);
        if (model.saveRecords() != 0) {
            model.deleteRecord(r.getRecordId());
            return "Could not save the record to file.";
        }
        fireChanged();
        return null;
    }

    public String handleUpdateRecord(String recordId, String studentNo, LocalDate date,
                                     LocalTime timeIn, AttendanceStatus status, String remarks) {
        AttendanceRecord r = model.findRecord(recordId);
        if (r == null) return "Record not found.";
        String err = validate(studentNo, date, timeIn, status, recordId);
        if (err != null) return err;
        r.setStudentNo(studentNo);
        r.setDate(date);
        r.setTimeIn(status.needsTimeIn() ? timeIn : null);
        r.setStatus(status);
        r.setRemarks(remarks == null ? "" : remarks.trim());
        if (model.saveRecords() != 0) return "Could not save the record to file.";
        fireChanged();
        return null;
    }

    public String handleDeleteRecord(String recordId) {
        if (model.deleteRecord(recordId) == null) return "Record not found.";
        if (model.saveRecords() != 0) return "Could not save the change to file.";
        fireChanged();
        return null;
    }

    public List<AttendanceRecord> getAllRecords() {
        return new ArrayList<>(model.findAll().values());
    }

    public AttendanceRecord getRecordDetails(String id) { return model.findRecord(id); }
}
