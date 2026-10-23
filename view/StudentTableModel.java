package view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;
import model.Section;
import model.Student;

public class StudentTableModel extends AbstractTableModel {
    private List<Student> students = new ArrayList<>();
    private Map<Integer, Section> sectionMap = new HashMap<>();
    private final String[] COLUMN_NAMES = {"Student No.", "Name", "Section"};

    @Override public int getRowCount() { return students.size(); }
    @Override public int getColumnCount() { return COLUMN_NAMES.length; }
    @Override public String getColumnName(int c) { return COLUMN_NAMES[c]; }

    @Override
    public Object getValueAt(int row, int col) {
        Student s = students.get(row);
        return switch (col) {
            case 0 -> s.getStudentNo();
            case 1 -> s.getName();
            case 2 -> {
                Section sec = sectionMap.get(s.getSectionId());
                yield sec != null ? sec.getName() : "No section";
            }
            default -> null;
        };
    }

    public void setData(List<Student> students, List<Section> sections) {
        this.students = students;
        this.sectionMap = new HashMap<>();
        for (Section s : sections) sectionMap.put(s.getId(), s);
        fireTableDataChanged();
    }

    public String getStudentNo(int row) { return students.get(row).getStudentNo(); }
}
