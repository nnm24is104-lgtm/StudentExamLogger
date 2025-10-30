package com.studentscores;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main application demonstrating concurrent student exam score submission
 * Uses multiple threads to simulate simultaneous score submissions
 */
public class ExamScoreLoggerApp {
    private static final String LOG_FILE_PATH = "data/student_scores.csv";
    private static final int NUMBER_OF_STUDENTS = 20;
    private static final int THREAD_POOL_SIZE = 5;

    public static void main(String[] args) {
        System.out.println("=== Student Exam Score Logger ===");
        System.out.println("Initializing score logging system...\n");

        ScoreLogger scoreLogger = new ScoreLogger(LOG_FILE_PATH);
        System.out.println("Log file created at: " + LOG_FILE_PATH);

        // Create thread pool for concurrent execution
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        System.out.println("\nSimulating " + NUMBER_OF_STUDENTS
                + " students submitting scores concurrently using " + THREAD_POOL_SIZE + " threads...\n");

        // Submit tasks for each student
        for (int i = 1; i <= NUMBER_OF_STUDENTS; i++) {
            Student student = StudentScoreSubmitter.generateRandomStudent(i);
            StudentScoreSubmitter submitter = new StudentScoreSubmitter(scoreLogger, student, 100);
            executorService.submit(submitter);
        }

        // Shutdown executor and wait for all tasks to complete
        executorService.shutdown();
        try {
            boolean finished = executorService.awaitTermination(30, TimeUnit.SECONDS);
            if (finished) {
                System.out.println("\n=== All submissions completed successfully ===");
                int entryCount = scoreLogger.getEntryCount();
                System.out.println("Total scores logged: " + entryCount);
                System.out.println("Check the file at: " + LOG_FILE_PATH);
            } else {
                System.out.println("\nTimeout: Not all submissions completed in time");
            }
        } catch (Exception e) {
            System.err.println("Error during execution: " + e.getMessage());
        }
    }

    /**
     * Run a demo with custom parameters
     */
    public static void runDemo(String filePath, int numStudents, int threadPoolSize) throws Exception {
        ScoreLogger scoreLogger = new ScoreLogger(filePath);
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        for (int i = 1; i <= numStudents; i++) {
            Student student = StudentScoreSubmitter.generateRandomStudent(i);
            StudentScoreSubmitter submitter = new StudentScoreSubmitter(scoreLogger, student);
            executorService.submit(submitter);
        }

        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);
    }
}