package com.studentscores;

import java.io.IOException;
import java.util.Random;

/**
 * Runnable task that simulates a student submitting their exam score
 * Multiple instances can run concurrently to test thread safety
 */
public class StudentScoreSubmitter implements Runnable {
    private final ScoreLogger scoreLogger;
    private final Student student;
    private final int delayMillis;

    public StudentScoreSubmitter(ScoreLogger scoreLogger, Student student, int delayMillis) {
        this.scoreLogger = scoreLogger;
        this.student = student;
        this.delayMillis = delayMillis;
    }

    public StudentScoreSubmitter(ScoreLogger scoreLogger, Student student) {
        this(scoreLogger, student, 0);
    }

    @Override
    public void run() {
        try {
            // Simulate processing time before submission
            if (delayMillis > 0) {
                Thread.sleep(delayMillis);
            }

            System.out.println(Thread.currentThread().getName() + " - Submitting score for: "
                    + student.getStudentName());

            scoreLogger.logScore(student);

            System.out.println(Thread.currentThread().getName() + " - Successfully logged score for: "
                    + student.getStudentName());

        } catch (IOException e) {
            System.err.println(Thread.currentThread().getName()
                    + " - Error logging score: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println(Thread.currentThread().getName()
                    + " - Thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Generate a random student for testing purposes
     */
    public static Student generateRandomStudent(int studentNumber) {
        Random random = new Random();
        String[] subjects = {"Mathematics", "Physics", "Chemistry", "Biology", "English", "History"};
        String[] firstNames = {"Alex", "Jamie", "Taylor", "Morgan", "Jordan", "Casey", "Riley", "Avery"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis"};

        String studentId = "STU" + String.format("%04d", studentNumber);
        String firstName = firstNames[random.nextInt(firstNames.length)];
        String lastName = lastNames[random.nextInt(lastNames.length)];
        String fullName = firstName + " " + lastName;
        int score = random.nextInt(51) + 50; // Score between 50 and 100
        String subject = subjects[random.nextInt(subjects.length)];

        return new Student(studentId, fullName, score, subject);
    }
}
