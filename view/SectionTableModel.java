package view;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import model.Section;

public class SectionTableModel extends AbstractTableModel {
    private List<Section> sections = new ArrayList<>();
    private final String[] COLUMN_NAMES = {"ID", "Section"};

    @Override public int getRowCount() { return sections.size(); }
    @Override public int getColumnCount() { return COLUMN_NAMES.length; }
    @Override public String getColumnName(int c) { return COLUMN_NAMES[c]; }

    // FIX: report real column types so the sorter sorts ID numerically (1, 2, 10 – not 1, 10, 2)
    @Override public Class<?> getColumnClass(int c) { return c == 0 ? Integer.class : String.class; }

    @Override
    public Object getValueAt(int row, int col) {
        Section s = sections.get(row);
        return col == 0 ? s.getId() : s.getName();
    }

    public void setSections(List<Section> sections) {
        this.sections = sections;
        fireTableDataChanged();
    }

    public int getSectionId(int row) { return sections.get(row).getId(); }
}
