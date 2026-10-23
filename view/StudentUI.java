/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package view;

import controller.SectionController;
import controller.StudentController;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableRowSorter;
import model.Section;
import model.Student;

/**
 * Manage > Students window (add / update / delete). Click a column header to sort.
 */
public class StudentUI extends javax.swing.JFrame {

    /**
     * Creates new form StudentUI
     */
    public StudentUI(StudentController controller, SectionController sectionController) {
        this.controller = controller;
        this.sectionController = sectionController;
        this.tableModel = new StudentTableModel();
        initComponents();

        setupListTable();
        setupRowsActionListener();

        controller.addListener(this::refreshStudents);
        sectionController.addListener(this::refreshStudents);
        refreshStudents();
        setDefaults();
    }

    private void setDefaults() {
        this.selectedNo = null;
        this.jTFStudentNo.setText("");
        this.jTFStudentNo.setEnabled(true);
        this.jTFStudentName.setText("");
        if (jComboBoxSection.getItemCount() > 0) {
            jComboBoxSection.setSelectedIndex(0);
        }
        this.jBtnAdd.setEnabled(true);
        this.jBtnUpdate.setEnabled(false);
        this.jBtnDelete.setEnabled(false);
    }

    private void setupListTable() {
        jTableStudents.setModel(tableModel);
        jTableStudents.setRowSorter(new TableRowSorter<>(tableModel)); // click header to sort
        jTableStudents.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jTableStudents.getTableHeader().setReorderingAllowed(false);
        jTableStudents.createDefaultColumnsFromModel();
        jTableStudents.getColumnModel().getColumn(0).setPreferredWidth(110);
        jTableStudents.getColumnModel().getColumn(1).setPreferredWidth(240);
        jTableStudents.getColumnModel().getColumn(2).setPreferredWidth(120);
    }

    private void setupRowsActionListener() {
        jTableStudents.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int selectedRow = jTableStudents.getSelectedRow();
            if (selectedRow != -1) {
                int modelRow = jTableStudents.convertRowIndexToModel(selectedRow);
                Student s = controller.getStudentDetails(tableModel.getStudentNo(modelRow));
                if (s != null) {
                    displaySelectedStudent(s);
                }
            }
        });
    }

    private void displaySelectedStudent(Student s) {
        this.selectedNo = s.getStudentNo();
        this.jTFStudentNo.setText(s.getStudentNo());
        this.jTFStudentNo.setEnabled(false); // Student No. is the key, it can't change on update
        this.jTFStudentName.setText(s.getName());
        this.jComboBoxSection.setSelectedItem(sectionController.getSectionDetails(s.getSectionId()));
        this.jBtnAdd.setEnabled(false);
        this.jBtnUpdate.setEnabled(true);
        this.jBtnDelete.setEnabled(true);
    }

    public void refreshStudents() {
        List<Section> sections = sectionController.getAllSections();
        Section keep = (Section) jComboBoxSection.getSelectedItem();
        jComboBoxSection.setModel(new DefaultComboBoxModel<>(sections.toArray(new Section[0])));
        if (keep != null && sections.contains(keep)) {
            jComboBoxSection.setSelectedItem(keep);
        }
        tableModel.setData(controller.getAllStudents(), sections);
    }

    private void save() {
        Section sec = (Section) jComboBoxSection.getSelectedItem();
        if (sec == null) {
            JOptionPane.showMessageDialog(this, "Add a section first (Manage > Sections).",
                    "Students", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String err = (selectedNo == null)
                ? controller.handleAddStudent(jTFStudentNo.getText(), jTFStudentName.getText(), sec.getId())
                : controller.handleUpdateStudent(selectedNo, jTFStudentName.getText(), sec.getId());
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Students", JOptionPane.ERROR_MESSAGE);
            return;
        }
        jTableStudents.clearSelection();
        setDefaults();
    }

    private void delete() {
        if (selectedNo == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this,
                "Delete this student and all of their attendance records?", "Students",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        String err = controller.handleDeleteStudent(selectedNo);
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "Students", JOptionPane.ERROR_MESSAGE);
            return;
        }
        jTableStudents.clearSelection();
        setDefaults();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTFStudentNo = new javax.swing.JTextField();
        jTFStudentName = new javax.swing.JTextField();
        jComboBoxSection = new javax.swing.JComboBox<>();
        jBtnAdd = new javax.swing.JButton();
        jBtnUpdate = new javax.swing.JButton();
        jBtnDelete = new javax.swing.JButton();
        jBtnClear = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableStudents = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Manage Students");

        jTFStudentNo.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jTFStudentNo.setBorder(javax.swing.BorderFactory.createTitledBorder("Student No. (e.g. 2024-0001)"));

        jTFStudentName.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jTFStudentName.setBorder(javax.swing.BorderFactory.createTitledBorder("Full name"));

        jComboBoxSection.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jComboBoxSection.setBorder(javax.swing.BorderFactory.createTitledBorder("Section"));

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

        jTableStudents.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(jTableStudents);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTFStudentNo, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTFStudentName, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxSection, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jBtnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBtnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBtnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBtnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 640, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTFStudentNo, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTFStudentName, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jComboBoxSection, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jBtnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtnUpdate, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtnDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtnClear, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jBtnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnAddActionPerformed
        save();
    }//GEN-LAST:event_jBtnAddActionPerformed

    private void jBtnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnUpdateActionPerformed
        save();
    }//GEN-LAST:event_jBtnUpdateActionPerformed

    private void jBtnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDeleteActionPerformed
        delete();
    }//GEN-LAST:event_jBtnDeleteActionPerformed

    private void jBtnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnClearActionPerformed
        jTableStudents.clearSelection();
        setDefaults();
    }//GEN-LAST:event_jBtnClearActionPerformed

    private final StudentTableModel tableModel;
    private final StudentController controller;
    private final SectionController sectionController;
    private String selectedNo = null;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBtnAdd;
    private javax.swing.JButton jBtnClear;
    private javax.swing.JButton jBtnDelete;
    private javax.swing.JButton jBtnUpdate;
    private javax.swing.JComboBox<Section> jComboBoxSection;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTFStudentName;
    private javax.swing.JTextField jTFStudentNo;
    private javax.swing.JTable jTableStudents;
    // End of variables declaration//GEN-END:variables
}
