package MainClass;

import controller.AttendanceController;
import controller.SectionController;
import controller.StudentController;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import model.AttendanceManager;
import model.SectionManager;
import model.StudentManager;
import utils.FileDBHelper;
import view.MainUI;

public class MainClass {

    public static void main(String[] args) {
        // 1. Initialize project directory
        FileDBHelper.createDirectory();

        // 2. Look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            Logger.getLogger(MainClass.class.getName()).log(Level.WARNING, "Could not set system L&F", ex);
        }

        // 3. Models (load persisted data on startup)
        SectionManager sectionManager = new SectionManager();
        StudentManager studentManager = new StudentManager();
        AttendanceManager attendanceManager = new AttendanceManager();
        sectionManager.loadSections();
        studentManager.loadStudents();
        attendanceManager.loadRecords();

        // 4. Controllers, injecting the models
        SectionController sectionController = new SectionController(sectionManager, studentManager);
        StudentController studentController =
                new StudentController(studentManager, sectionManager, attendanceManager);
        AttendanceController attendanceController =
                new AttendanceController(attendanceManager, studentManager);

        // 5. Launch the view on the Event Dispatch Thread
        java.awt.EventQueue.invokeLater(() -> {
            MainUI view = new MainUI(attendanceController, studentController, sectionController);
            view.setVisible(true);
        });
    }
}
