package com.studentscores;

/**
 * Student entity representing a student with their exam score
 */
public class Student {
    private final String studentId;
    private final String studentName;
    private final int examScore;
    private final String subject;

    public Student(String studentId, String studentName, int examScore, String subject) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.examScore = examScore;
        this.subject = subject;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public int getExamScore() {
        return examScore;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%d,%s", studentId, studentName, examScore, subject);
    }
}
