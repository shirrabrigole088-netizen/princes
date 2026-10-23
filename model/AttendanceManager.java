package model;

import java.time.LocalDate;
import java.util.HashMap;
import utils.FileDBHelper;

public class AttendanceManager {
    private HashMap<String, AttendanceRecord> records = new HashMap<>();
    private final String FILE_PATH = "attendance.ser";

    public AttendanceManager() {
        FileDBHelper.createFile(FILE_PATH);
    }

    public void addRecord(AttendanceRecord r) { records.put(r.getRecordId(), r); }
    public AttendanceRecord findRecord(String id) { return records.get(id); }
    public AttendanceRecord deleteRecord(String id) { return records.remove(id); }
    public HashMap<String, AttendanceRecord> findAll() { return records; }

    /** True if the student already has a record on that date (ignoring record 'exceptId'). */
    public boolean existsFor(String studentNo, LocalDate date, String exceptId) {
        return records.values().stream().anyMatch(r ->
                r.getStudentNo().equals(studentNo) && r.getDate().equals(date)
                        && !r.getRecordId().equals(exceptId));
    }

    public void deleteByStudent(String studentNo) {
        records.values().removeIf(r -> r.getStudentNo().equals(studentNo));
    }

    public int saveRecords() { return FileDBHelper.save(FILE_PATH, records); }

    @SuppressWarnings("unchecked")
    public void loadRecords() {
        Object o = FileDBHelper.load(FILE_PATH);
        if (o != null) records = (HashMap<String, AttendanceRecord>) o;
    }
}
