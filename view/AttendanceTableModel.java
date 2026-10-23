package view;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;
import model.AttendanceRecord;
import model.AttendanceStatus;
import model.Section;
import model.Student;

public class AttendanceTableModel extends AbstractTableModel {
    public static final int COL_DATE = 0, COL_STUDENT_NO = 1, COL_NAME = 2,
            COL_SECTION = 3, COL_TIME_IN = 4, COL_STATUS = 5, COL_REMARKS = 6;

    private List<AttendanceRecord> records = new ArrayList<>();
    private Map<String, Student> studentMap = new HashMap<>();
    private Map<Integer, Section> sectionMap = new HashMap<>();

    private final String[] COLUMN_NAMES = {"Date", "Student No.", "Student Name",
        "Section", "Time In", "Status", "Remarks"};

    @Override public int getRowCount() { return records.size(); }
    @Override public int getColumnCount() { return COLUMN_NAMES.length; }
    @Override public String getColumnName(int c) { return COLUMN_NAMES[c]; }

    /** Real column types => Date/Time/Status sort properly (not as text). */
    @Override
    public Class<?> getColumnClass(int c) {
        return switch (c) {
            case COL_DATE -> LocalDate.class;
            case COL_TIME_IN -> LocalTime.class;
            case COL_STATUS -> AttendanceStatus.class;
            default -> String.class;
        };
    }

    @Override
    public Object getValueAt(int row, int col) {
        AttendanceRecord r = records.get(row);
        Student st = studentMap.get(r.getStudentNo());
        return switch (col) {
            case COL_DATE -> r.getDate();
            case COL_STUDENT_NO -> r.getStudentNo();
            case COL_NAME -> st != null ? st.getName() : "(deleted student)";
            case COL_SECTION -> {
                Section sec = st != null ? sectionMap.get(st.getSectionId()) : null;
                yield sec != null ? sec.getName() : "-";
            }
            case COL_TIME_IN -> r.getTimeIn();
            case COL_STATUS -> r.getStatus();
            case COL_REMARKS -> r.getRemarks();
            default -> null;
        };
    }

    public void setData(List<AttendanceRecord> records, List<Student> students, List<Section> sections) {
        this.records = records;
        this.studentMap = new HashMap<>();
        for (Student s : students) studentMap.put(s.getStudentNo(), s);
        this.sectionMap = new HashMap<>();
        for (Section s : sections) sectionMap.put(s.getId(), s);
        fireTableDataChanged();
    }

    public String getRecordId(int row) { return records.get(row).getRecordId(); }
    public AttendanceRecord getRecord(int row) { return records.get(row); }
}
