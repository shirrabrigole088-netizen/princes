/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package view;

import controller.AttendanceController;
import controller.SectionController;
import controller.StudentController;
import java.awt.Component;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import model.AttendanceRecord;
import model.AttendanceStatus;
import model.Student;

/**
 * Main window: record daily attendance, then sort / filter / export the log.
 */
public class MainUI extends javax.swing.JFrame {

    private static final String TITLE = "Attendance Monitoring System";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Creates new form MainUI
     */
    public MainUI(AttendanceController controller, StudentController studentController,
            SectionController sectionController) {
        this.controller = controller;
        this.studentController = studentController;
        this.sectionController = sectionController;
        this.tableModel = new AttendanceTableModel();

        initComponents();
        setupListTable();
        setupRowsActionListener();
        setupFilterListener();
        jComboBoxStatus.setModel(new DefaultComboBoxModel<>(AttendanceStatus.values()));
        setLocationRelativeTo(null);

        // refresh whenever any controller reports a change
        Runnable refresh = this::refreshAll;
        controller.addListener(refresh);
        studentController.addListener(refresh);
        sectionController.addListener(refresh);

        setUpDefaults();
        refreshAll();
        applySort(AttendanceTableModel.COL_DATE, SortOrder.DESCENDING); // newest first
    }

    //default display
    private void setUpDefaults() {
        this.selectedRecordId = null;
        if (jComboBoxStudent.getItemCount() > 0) {
            jComboBoxStudent.setSelectedIndex(0);
        }
        this.jTFDate.setText(LocalDate.now().format(DATE_FMT));
        this.jTFTime.setText(LocalTime.now().format(TIME_FMT));
        this.jComboBoxStatus.setSelectedItem(AttendanceStatus.PRESENT);
        this.jTARemarks.setText("");
        this.jBtnAdd.setEnabled(true);   // FIX: the sample never re-enabled Add
        this.jBtnUpdate.setEnabled(false);
        this.jBtnDelete.setEnabled(false);
        updateTimeFieldState();
    }

    private void updateTimeFieldState() {
        AttendanceStatus s = (AttendanceStatus) jComboBoxStatus.getSelectedItem();
        boolean need = s != null && s.needsTimeIn();
        jTFTime.setEnabled(need);
        if (need && jTFTime.getText().isBlank()) {
            jTFTime.setText(LocalTime.now().format(TIME_FMT));
        }
    }

    //setup table model + sorter + renderers
    private void setupListTable() {
        jTableRecords.setModel(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        jTableRecords.setRowSorter(sorter);              // enables click-header sorting
        jTableRecords.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jTableRecords.getTableHeader().setReorderingAllowed(false);
        jTableRecords.createDefaultColumnsFromModel();
        jTableRecords.setRowHeight(24);
        jTableRecords.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // header click => the Sort menu radio no longer matches, so clear it
        sorter.addRowSorterListener(e -> {
            if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED && !applyingMenuSort) {
                buttonGroupSort.clearSelection();
            }
            updateSummary();
        });

        DefaultTableCellRenderer dateRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object v) {
                setText(v instanceof LocalDate d ? d.format(DATE_FMT) : "");
            }
        };
        DefaultTableCellRenderer timeRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object v) {
                setText(v instanceof LocalTime t ? t.format(TIME_FMT) : "-");
            }
        };
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                    boolean focus, int row, int col) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, focus, row, col);
                if (!sel && v instanceof AttendanceStatus s) {
                    comp.setForeground(switch (s) {
                        case PRESENT -> new Color(0x1B7F3B);
                        case LATE -> new Color(0xB36B00);
                        case ABSENT -> new Color(0xC62828);
                        case EXCUSED -> new Color(0x1565C0);
                    });
                } else if (!sel) {
                    comp.setForeground(t.getForeground());
                }
                return comp;
            }
        };
        javax.swing.table.TableColumnModel cm = jTableRecords.getColumnModel();
        cm.getColumn(AttendanceTableModel.COL_DATE).setCellRenderer(dateRenderer);
        cm.getColumn(AttendanceTableModel.COL_TIME_IN).setCellRenderer(timeRenderer);
        cm.getColumn(AttendanceTableModel.COL_STATUS).setCellRenderer(statusRenderer);
        int[] widths = {95, 95, 170, 100, 70, 80, 200};
        for (int i = 0; i < widths.length; i++) {
            cm.getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private void setupRowsActionListener() {
        jTableRecords.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return; // FIX: the sample wiped the form while the selection was still adjusting
            }
            int selectedRow = jTableRecords.getSelectedRow();
            if (selectedRow != -1) {
                int modelRow = jTableRecords.convertRowIndexToModel(selectedRow);
                AttendanceRecord r = controller.getRecordDetails(tableModel.getRecordId(modelRow));
                if (r != null) {
                    displaySelectedRecord(r);
                }
            }
        });
    }

    private void setupFilterListener() {
        jTFFilter.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
    }

    private void displaySelectedRecord(AttendanceRecord r) {
        this.selectedRecordId = r.getRecordId();
        for (int i = 0; i < jComboBoxStudent.getItemCount(); i++) {
            if (jComboBoxStudent.getItemAt(i).getStudentNo().equals(r.getStudentNo())) {
                jComboBoxStudent.setSelectedIndex(i);
                break;
            }
        }
        this.jTFDate.setText(r.getDate().format(DATE_FMT));
        this.jTFTime.setText(r.getTimeIn() == null ? "" : r.getTimeIn().format(TIME_FMT));
        this.jComboBoxStatus.setSelectedItem(r.getStatus());
        this.jTARemarks.setText(r.getRemarks());
        this.jBtnAdd.setEnabled(false);
        this.jBtnUpdate.setEnabled(true);
        this.jBtnDelete.setEnabled(true);
        updateTimeFieldState();
    }

    // ------------------------------------------------------------ sorting / filtering
    /** Sorts the table by one column (+ tie-breakers). Used by the Sort menu. */
    private void applySort(int col, SortOrder order) {
        applyingMenuSort = true;
        try {
            List<RowSorter.SortKey> keys = new ArrayList<>();
            keys.add(new RowSorter.SortKey(col, order));
            if (col != AttendanceTableModel.COL_DATE) {
                keys.add(new RowSorter.SortKey(AttendanceTableModel.COL_DATE, SortOrder.DESCENDING));
            }
            if (col != AttendanceTableModel.COL_NAME) {
                keys.add(new RowSorter.SortKey(AttendanceTableModel.COL_NAME, SortOrder.ASCENDING));
            }
            sorter.setSortKeys(keys);
        } finally {
            applyingMenuSort = false;
        }
    }

    private void applyFilter() {
        String text = jTFFilter.getText().trim();
        sorter.setRowFilter(text.isEmpty() ? null
                : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        updateSummary();
    }

    private void updateSummary() {
        int[] counts = new int[AttendanceStatus.values().length];
        for (int i = 0; i < jTableRecords.getRowCount(); i++) {
            AttendanceRecord r = tableModel.getRecord(jTableRecords.convertRowIndexToModel(i));
            counts[r.getStatus().ordinal()]++;
        }
        jLblSummary.setText(String.format(
                "Showing %d of %d records   |   Present: %d   Late: %d   Absent: %d   Excused: %d",
                jTableRecords.getRowCount(), tableModel.getRowCount(),
                counts[AttendanceStatus.PRESENT.ordinal()], counts[AttendanceStatus.LATE.ordinal()],
                counts[AttendanceStatus.ABSENT.ordinal()], counts[AttendanceStatus.EXCUSED.ordinal()]));
    }

    /** Re-reads everything from the controllers (called after any add/update/delete). */
    public void refreshAll() {
        Student prev = (Student) jComboBoxStudent.getSelectedItem();
        List<Student> students = studentController.getAllStudents();
        jComboBoxStudent.setModel(new DefaultComboBoxModel<>(students.toArray(new Student[0])));
        if (prev != null) {
            for (Student s : students) {
                if (s.getStudentNo().equals(prev.getStudentNo())) {
                    jComboBoxStudent.setSelectedItem(s);
                    break;
                }
            }
        }
        String savedId = this.selectedRecordId;
        tableModel.setData(controller.getAllRecords(), students, sectionController.getAllSections());
        updateSummary();

        if (savedId != null) {
            int modelRow = -1;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (savedId.equals(tableModel.getRecordId(i))) {
                    modelRow = i;
                    break;
                }
            }
            int viewRow = (modelRow == -1) ? -1 : jTableRecords.convertRowIndexToView(modelRow);
            if (viewRow != -1) {
                jTableRecords.setRowSelectionInterval(viewRow, viewRow);
            } else {
                setUpDefaults();
            }
        }
    }

    // ------------------------------------------------------------ actions
    /** Reads the form. Returns null (after showing a message) when date/time can't be parsed. */
    private Object[] readForm() {
        LocalDate date;
        try {
            date = LocalDate.parse(jTFDate.getText().trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            error("Invalid date. Use the format yyyy-MM-dd, e.g. 2026-10-23.");
            return null;
        }
        LocalTime time = null;
        AttendanceStatus status = (AttendanceStatus) jComboBoxStatus.getSelectedItem();
        if (status != null && status.needsTimeIn()) {
            try {
                time = LocalTime.parse(jTFTime.getText().trim(), TIME_FMT);
            } catch (DateTimeParseException ex) {
                error("Invalid time. Use 24-hour HH:mm, e.g. 07:45 or 13:05.");
                return null;
            }
        }
        Student st = (Student) jComboBoxStudent.getSelectedItem();
        return new Object[]{st == null ? null : st.getStudentNo(), date, time, status, jTARemarks.getText()};
    }

    private void onAdd() {
        if (jComboBoxStudent.getItemCount() == 0) {
            error("No students yet. Add students first (Manage > Students).");
            return;
        }
        Object[] f = readForm();
        if (f == null) {
            return;
        }
        String err = controller.handleAddRecord((String) f[0], (LocalDate) f[1], (LocalTime) f[2],
                (AttendanceStatus) f[3], (String) f[4]);
        if (err != null) {
            error(err);
            return;
        }
        info("Attendance recorded");
        setUpDefaults();
    }

    private void onUpdate() {
        if (selectedRecordId == null) {
            return;
        }
        Object[] f = readForm();
        if (f == null) {
            return;
        }
        String err = controller.handleUpdateRecord(selectedRecordId, (String) f[0], (LocalDate) f[1],
                (LocalTime) f[2], (AttendanceStatus) f[3], (String) f[4]);
        if (err != null) {
            error(err);
            return;
        }
        info("Attendance record updated");
        jTableRecords.clearSelection();
        setUpDefaults();
    }

    private void onDelete() {
        if (selectedRecordId == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete this attendance record?", TITLE,
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        String err = controller.handleDeleteRecord(selectedRecordId);
        if (err != null) {
            error(err);
            return;
        }
        info("Attendance record deleted");
        jTableRecords.clearSelection();
        setUpDefaults();
    }

    private void openStudents() {
        if (studentForm == null || !studentForm.isDisplayable()) {
            studentForm = new StudentUI(studentController, sectionController);
        }
        studentForm.setLocationRelativeTo(this);
        studentForm.setVisible(true);
        studentForm.toFront();
    }

    private void openSections() {
        if (sectionForm == null || !sectionForm.isDisplayable()) {
            sectionForm = new SectionUI(sectionController);
        }
        sectionForm.setLocationRelativeTo(this);
        sectionForm.setVisible(true);
        sectionForm.toFront();
    }

    /** Exports exactly what the table shows now (current filter + sort order). */
    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("attendance.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try (PrintWriter out = new PrintWriter(fc.getSelectedFile(), "UTF-8")) {
            StringBuilder head = new StringBuilder();
            for (int c = 0; c < jTableRecords.getColumnCount(); c++) {
                if (c > 0) {
                    head.append(',');
                }
                head.append(csv(jTableRecords.getColumnName(c)));
            }
            out.println(head);
            for (int r = 0; r < jTableRecords.getRowCount(); r++) {
                int modelRow = jTableRecords.convertRowIndexToModel(r);
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    if (c > 0) {
                        sb.append(',');
                    }
                    Object v = tableModel.getValueAt(modelRow, c);
                    String s = v instanceof LocalDate d ? d.format(DATE_FMT)
                            : v instanceof LocalTime t ? t.format(TIME_FMT)
                            : v == null ? "" : v.toString();
                    sb.append(csv(s));
                }
                out.println(sb);
            }
            info("Exported " + jTableRecords.getRowCount() + " rows");
        } catch (IOException ex) {
            error("Could not write file: " + ex.getMessage());
        }
    }

    private static String csv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, TITLE, JOptionPane.ERROR_MESSAGE);
    }

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, TITLE, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupSort = new javax.swing.ButtonGroup();

        jLabel1 = new javax.swing.JLabel();
        jComboBoxStudent = new javax.swing.JComboBox<>();
        jComboBoxStatus = new javax.swing.JComboBox<>();
        jTFDate = new javax.swing.JTextField();
        jTFTime = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTARemarks = new javax.swing.JTextArea();
        jBtnAdd = new javax.swing.JButton();
        jBtnUpdate = new javax.swing.JButton();
        jBtnDelete = new javax.swing.JButton();
        jBtnClear = new javax.swing.JButton();
        jTFFilter = new javax.swing.JTextField();
        jLblSummary = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableRecords = new javax.swing.JTable();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemExport = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenuManage = new javax.swing.JMenu();
        jMenuItemStudents = new javax.swing.JMenuItem();
        jMenuItemSections = new javax.swing.JMenuItem();
        jMenuSort = new javax.swing.JMenu();
        jRadioSortDateNew = new javax.swing.JRadioButtonMenuItem();
        jRadioSortDateOld = new javax.swing.JRadioButtonMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jRadioSortNameAZ = new javax.swing.JRadioButtonMenuItem();
        jRadioSortNameZA = new javax.swing.JRadioButtonMenuItem();
        jRadioSortStudentNo = new javax.swing.JRadioButtonMenuItem();
        jRadioSortSection = new javax.swing.JRadioButtonMenuItem();
        jRadioSortStatus = new javax.swing.JRadioButtonMenuItem();
        jRadioSortTimeEarly = new javax.swing.JRadioButtonMenuItem();
        jRadioSortTimeLate = new javax.swing.JRadioButtonMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Attendance Monitoring System");

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Attendance Monitoring System");

        jComboBoxStudent.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jComboBoxStudent.setBorder(javax.swing.BorderFactory.createTitledBorder("Student"));

        jComboBoxStatus.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jComboBoxStatus.setBorder(javax.swing.BorderFactory.createTitledBorder("Status"));
        jComboBoxStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxStatusActionPerformed(evt);
            }
        });

        jTFDate.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jTFDate.setBorder(javax.swing.BorderFactory.createTitledBorder("Date (yyyy-MM-dd)"));

        jTFTime.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jTFTime.setBorder(javax.swing.BorderFactory.createTitledBorder("Time In (HH:mm, 24h)"));

        jTARemarks.setColumns(20);
        jTARemarks.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jTARemarks.setLineWrap(true);
        jTARemarks.setWrapStyleWord(true);
        jTARemarks.setRows(3);
        jTARemarks.setBorder(javax.swing.BorderFactory.createTitledBorder("Remarks (optional)"));
        jScrollPane1.setViewportView(jTARemarks);

        jBtnAdd.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jBtnAdd.setText("Add");
        jBtnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnAddActionPerformed(evt);
            }
        });

        jBtnUpdate.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jBtnUpdate.setText("Update");
        jBtnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnUpdateActionPerformed(evt);
            }
        });

        jBtnDelete.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jBtnDelete.setText("Delete");
        jBtnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDeleteActionPerformed(evt);
            }
        });

        jBtnClear.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        jBtnClear.setText("Clear");
        jBtnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnClearActionPerformed(evt);
            }
        });

        jTFFilter.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jTFFilter.setBorder(javax.swing.BorderFactory.createTitledBorder("Search / filter (name, date, section, status...)"));

        jLblSummary.setText(" ");

        jScrollPane2.setBorder(javax.swing.BorderFactory.createTitledBorder("Attendance Log"));
        jTableRecords.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jScrollPane2.setViewportView(jTableRecords);

        jMenuFile.setText("File");
        jMenuItemExport.setText("Export shown rows to CSV...");
        jMenuItemExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExportActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemExport);

        jMenuFile.add(jSeparator1);

        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemExit);

        jMenuBar1.add(jMenuFile);

        jMenuManage.setText("Manage");
        jMenuItemStudents.setText("Students");
        jMenuItemStudents.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStudentsActionPerformed(evt);
            }
        });
        jMenuManage.add(jMenuItemStudents);

        jMenuItemSections.setText("Sections");
        jMenuItemSections.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSectionsActionPerformed(evt);
            }
        });
        jMenuManage.add(jMenuItemSections);

        jMenuBar1.add(jMenuManage);

        jMenuSort.setText("Sort");
        buttonGroupSort.add(jRadioSortDateNew);
        jRadioSortDateNew.setSelected(true);
        jRadioSortDateNew.setText("Date: newest first");
        jRadioSortDateNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioSortDateNewActionPerformed(evt);
            }
        });
        jMenuSort.add(jRadioSortDateNew);

        buttonGroupSort.add(jRadioSortDateOld);
        jRadioSortDateOld.setText("Date: oldest first");
        jRadioSortDateOld.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioSortDateOldActionPerformed(evt);
            }
        });
        jMenuSort.add(jRadioSortDateOld);

        jMenuSort.add(jSeparator2);

        buttonGroupSort.add(jRadioSortNameAZ);
        jRadioSortNameAZ.setText("Student name: A to Z");
        jRadioSortNameAZ.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioSortNameAZActionPerformed(evt);
            }
        });
        jMenuSort.add(jRadioSortNameAZ);

        buttonGroupSort.add(jRadioSortNameZA);
        jRadioSortNameZA.setText("Student name: Z to A");
        jRadioSortNameZA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioSortNameZAActionPerformed(evt);
            }
        });
        jMenuSort.add(jRadioSortNameZA);

        buttonGroupSort.add(jRadioSortStudentNo);
        jRadioSortStudentNo.setText("Student No.");
        jRadioSortStudentNo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioSortStudentNoActionPerformed(evt);
            }
        });
        jMenuSort.add(jRadioSortStudentNo);

        buttonGroupSort.add(jRadioSortSection);
        jRadioSortSection.setText("Section");
        jRadioSortSection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioSortSectionActionPerformed(evt);
            }
        });
        jMenuSort.add(jRadioSortSection);

        buttonGroupSort.add(jRadioSortStatus);
        jRadioSortStatus.setText("Status");
        jRadioSortStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioSortStatusActionPerformed(evt);
            }
        });
        jMenuSort.add(jRadioSortStatus);

        buttonGroupSort.add(jRadioSortTimeEarly);
        jRadioSortTimeEarly.setText("Time in: earliest first");
        jRadioSortTimeEarly.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioSortTimeEarlyActionPerformed(evt);
            }
        });
        jMenuSort.add(jRadioSortTimeEarly);

        buttonGroupSort.add(jRadioSortTimeLate);
        jRadioSortTimeLate.setText("Time in: latest first");
        jRadioSortTimeLate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioSortTimeLateActionPerformed(evt);
            }
        });
        jMenuSort.add(jRadioSortTimeLate);

        jMenuBar1.add(jMenuSort);
        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jComboBoxStudent, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBtnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBtnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTFDate, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTFTime, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBtnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBtnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jTFFilter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLblSummary, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 860, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jComboBoxStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBoxStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTFDate, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTFTime, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTFFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLblSummary)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBoxStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxStatusActionPerformed
        updateTimeFieldState();
    }//GEN-LAST:event_jComboBoxStatusActionPerformed

    private void jBtnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnAddActionPerformed
        onAdd();
    }//GEN-LAST:event_jBtnAddActionPerformed

    private void jBtnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnUpdateActionPerformed
        onUpdate();
    }//GEN-LAST:event_jBtnUpdateActionPerformed

    private void jBtnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDeleteActionPerformed
        onDelete();
    }//GEN-LAST:event_jBtnDeleteActionPerformed

    private void jBtnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnClearActionPerformed
        jTableRecords.clearSelection();
        setUpDefaults();
    }//GEN-LAST:event_jBtnClearActionPerformed

    private void jMenuItemExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExportActionPerformed
        exportCsv();
    }//GEN-LAST:event_jMenuItemExportActionPerformed

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        dispose();
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jMenuItemStudentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemStudentsActionPerformed
        openStudents();
    }//GEN-LAST:event_jMenuItemStudentsActionPerformed

    private void jMenuItemSectionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSectionsActionPerformed
        openSections();
    }//GEN-LAST:event_jMenuItemSectionsActionPerformed

    private void jRadioSortDateNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioSortDateNewActionPerformed
        applySort(AttendanceTableModel.COL_DATE, SortOrder.DESCENDING);
    }//GEN-LAST:event_jRadioSortDateNewActionPerformed

    private void jRadioSortDateOldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioSortDateOldActionPerformed
        applySort(AttendanceTableModel.COL_DATE, SortOrder.ASCENDING);
    }//GEN-LAST:event_jRadioSortDateOldActionPerformed

    private void jRadioSortNameAZActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioSortNameAZActionPerformed
        applySort(AttendanceTableModel.COL_NAME, SortOrder.ASCENDING);
    }//GEN-LAST:event_jRadioSortNameAZActionPerformed

    private void jRadioSortNameZAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioSortNameZAActionPerformed
        applySort(AttendanceTableModel.COL_NAME, SortOrder.DESCENDING);
    }//GEN-LAST:event_jRadioSortNameZAActionPerformed

    private void jRadioSortStudentNoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioSortStudentNoActionPerformed
        applySort(AttendanceTableModel.COL_STUDENT_NO, SortOrder.ASCENDING);
    }//GEN-LAST:event_jRadioSortStudentNoActionPerformed

    private void jRadioSortSectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioSortSectionActionPerformed
        applySort(AttendanceTableModel.COL_SECTION, SortOrder.ASCENDING);
    }//GEN-LAST:event_jRadioSortSectionActionPerformed

    private void jRadioSortStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioSortStatusActionPerformed
        applySort(AttendanceTableModel.COL_STATUS, SortOrder.ASCENDING);
    }//GEN-LAST:event_jRadioSortStatusActionPerformed

    private void jRadioSortTimeEarlyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioSortTimeEarlyActionPerformed
        applySort(AttendanceTableModel.COL_TIME_IN, SortOrder.ASCENDING);
    }//GEN-LAST:event_jRadioSortTimeEarlyActionPerformed

    private void jRadioSortTimeLateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioSortTimeLateActionPerformed
        applySort(AttendanceTableModel.COL_TIME_IN, SortOrder.DESCENDING);
    }//GEN-LAST:event_jRadioSortTimeLateActionPerformed

    private final AttendanceTableModel tableModel;
    private final AttendanceController controller;
    private final StudentController studentController;
    private final SectionController sectionController;
    private TableRowSorter<AttendanceTableModel> sorter;
    private String selectedRecordId = null;
    private boolean applyingMenuSort = false;
    private StudentUI studentForm;
    private SectionUI sectionForm;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupSort;
    private javax.swing.JButton jBtnAdd;
    private javax.swing.JButton jBtnClear;
    private javax.swing.JButton jBtnDelete;
    private javax.swing.JButton jBtnUpdate;
    private javax.swing.JComboBox<AttendanceStatus> jComboBoxStatus;
    private javax.swing.JComboBox<Student> jComboBoxStudent;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLblSummary;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemExport;
    private javax.swing.JMenuItem jMenuItemSections;
    private javax.swing.JMenuItem jMenuItemStudents;
    private javax.swing.JMenu jMenuManage;
    private javax.swing.JMenu jMenuSort;
    private javax.swing.JRadioButtonMenuItem jRadioSortDateNew;
    private javax.swing.JRadioButtonMenuItem jRadioSortDateOld;
    private javax.swing.JRadioButtonMenuItem jRadioSortNameAZ;
    private javax.swing.JRadioButtonMenuItem jRadioSortNameZA;
    private javax.swing.JRadioButtonMenuItem jRadioSortSection;
    private javax.swing.JRadioButtonMenuItem jRadioSortStatus;
    private javax.swing.JRadioButtonMenuItem jRadioSortStudentNo;
    private javax.swing.JRadioButtonMenuItem jRadioSortTimeEarly;
    private javax.swing.JRadioButtonMenuItem jRadioSortTimeLate;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JTextArea jTARemarks;
    private javax.swing.JTextField jTFDate;
    private javax.swing.JTextField jTFFilter;
    private javax.swing.JTextField jTFTime;
    private javax.swing.JTable jTableRecords;
    // End of variables declaration//GEN-END:variables
}
