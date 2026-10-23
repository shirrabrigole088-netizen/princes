package controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import model.AttendanceManager;
import model.SectionManager;
import model.Student;
import model.StudentManager;

public class StudentController extends ChangeNotifier {
    private final StudentManager model;
    private final SectionManager sectionModel;
    private final AttendanceManager attendanceModel;

    public StudentController(StudentManager model, SectionManager sectionModel,
                             AttendanceManager attendanceModel) {
        this.model = model;
        this.sectionModel = sectionModel;
        this.attendanceModel = attendanceModel;
    }

    public String handleAddStudent(String studentNo, String name, int sectionId) {
        studentNo = studentNo == null ? "" : studentNo.trim();
        name = name == null ? "" : name.trim();
        if (studentNo.isEmpty() || name.isEmpty()) return "Student No. and name are required.";
        if (sectionModel.findSection(sectionId) == null) return "Please select a section.";
        if (model.findStudent(studentNo) != null)
            return "Student No. " + studentNo + " already exists.";
        Student s = new Student(studentNo, name, sectionId);
        model.addStudent(s);
        if (model.saveStudents() != 0) {
            model.deleteStudent(studentNo);
            return "Could not save the student to file.";
        }
        fireChanged();
        return null;
    }

    public String handleUpdateStudent(String studentNo, String name, int sectionId) {
        name = name == null ? "" : name.trim();
        if (name.isEmpty()) return "Student name is required.";
        if (sectionModel.findSection(sectionId) == null) return "Please select a section.";
        Student s = model.findStudent(studentNo);
        if (s == null) return "Student not found.";
        s.setName(name);
        s.setSectionId(sectionId);
        if (model.saveStudents() != 0) return "Could not save the student to file.";
        fireChanged();
        return null;
    }

    /** Deleting a student also deletes that student's attendance records. */
    public String handleDeleteStudent(String studentNo) {
        if (model.deleteStudent(studentNo) == null) return "Student not found.";
        attendanceModel.deleteByStudent(studentNo);
        if (model.saveStudents() != 0 || attendanceModel.saveRecords() != 0)
            return "Could not save the change to file.";
        fireChanged();
        return null;
    }

    public List<Student> getAllStudents() {
        List<Student> list = new ArrayList<>(model.findAll().values());
        list.sort(Comparator.comparing(Student::getName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public Student getStudentDetails(String studentNo) { return model.findStudent(studentNo); }
}
