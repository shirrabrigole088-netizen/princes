package model;

public enum AttendanceStatus {
    PRESENT("Present"), LATE("Late"), ABSENT("Absent"), EXCUSED("Excused");

    private final String label;
    AttendanceStatus(String label) { this.label = label; }

    /** Present/Late need a time-in; Absent/Excused do not. */
    public boolean needsTimeIn() { return this == PRESENT || this == LATE; }

    @Override
    public String toString() { return label; }
}
