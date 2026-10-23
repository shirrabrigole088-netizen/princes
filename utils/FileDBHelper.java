package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Small helper for file-based persistence (.ser files inside app/data/).
 */
public class FileDBHelper {
    public static final String DIRECTORY = "app/data/";

    public static String createDirectory() {
        File directory = new File(DIRECTORY);
        if (!directory.exists() && !directory.mkdirs()) {
            System.out.println("Error creating directories");
            return null;
        }
        return DIRECTORY;
    }

    public static String getDirectory() {
        return DIRECTORY;
    }

    /** Creates the data file (and the folder) if it does not exist yet. */
    public static String createFile(String fileName) {
        createDirectory(); // FIX: sample crashed if the folder was missing
        File f = new File(DIRECTORY + fileName);
        try {
            f.createNewFile();
            return f.getPath();
        } catch (IOException e) {
            System.out.println("Create File error: " + e);
            return null;
        }
    }

    /** @return 0 on success, -1 on error (same code for every manager). */
    public static int save(String fileName, Object data) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(DIRECTORY + fileName))) {
            oos.writeObject(data);
            return 0;
        } catch (IOException e) {
            System.out.println("Error Save: " + e);
            return -1;
        }
    }

    /** @return the stored object, or null if the file is new/empty/unreadable. */
    public static Object load(String fileName) {
        File f = new File(DIRECTORY + fileName);
        if (!f.exists() || f.length() == 0) {
            return null; // FIX: no more EOFException spam on first run
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error Read: " + e);
            return null;
        }
    }
}
