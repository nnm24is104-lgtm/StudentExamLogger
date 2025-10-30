package com.studentscores;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test cases for StudentScoreSubmitter
 * Tests thread execution and completion
 */
public class StudentScoreSubmitterTest {

    @TempDir
    Path tempDir;

    private ScoreLogger scoreLogger;
    private String testFilePath;

    @BeforeEach
    public void setUp() {
        testFilePath = tempDir.resolve("submitter_test_scores.csv").toString();
        scoreLogger = new ScoreLogger(testFilePath);
    }

    @AfterEach
    public void tearDown() throws IOException {
        Path path = Path.of(testFilePath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    @Test
    public void testSingleThreadSubmission() throws InterruptedException, IOException {
        Student student = new Student("STU001", "Test Student", 90, "Physics");
        StudentScoreSubmitter submitter = new StudentScoreSubmitter(scoreLogger, student);

        Thread thread = new Thread(submitter);
        thread.start();
        thread.join(5000); // Wait max 5 seconds

        assertFalse(thread.isAlive(), "Thread should complete execution");
        assertEquals(1, scoreLogger.getEntryCount(), "Score should be logged");
    }

    @Test
    public void testMultipleThreadsCompletion() throws InterruptedException, IOException {
        final int numThreads = 3;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            Student student = new Student("STU" + i, "Student " + i, 75 + i, "Math");
            StudentScoreSubmitter submitter = new StudentScoreSubmitter(scoreLogger, student);
            threads[i] = new Thread(submitter, "Thread-" + i);
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(5000);
            assertFalse(thread.isAlive(), "Thread " + thread.getName() + " should complete");
        }

        assertEquals(numThreads, scoreLogger.getEntryCount(),
            "All threads should successfully log their scores");
    }

    @Test
    public void testThreadPoolExecution() throws InterruptedException, IOException {
        final int numStudents = 10;
        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 0; i < numStudents; i++) {
            Student student = StudentScoreSubmitter.generateRandomStudent(i);
            StudentScoreSubmitter submitter = new StudentScoreSubmitter(scoreLogger, student);
            executor.submit(submitter);
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);

        assertTrue(finished, "All tasks should complete within timeout");
        assertEquals(numStudents, scoreLogger.getEntryCount(),
            "All student scores should be logged");
    }

    @Test
    public void testGenerateRandomStudent() {
        Student student1 = StudentScoreSubmitter.generateRandomStudent(1);
        Student student2 = StudentScoreSubmitter.generateRandomStudent(2);

        assertNotNull(student1, "Generated student should not be null");
        assertNotNull(student2, "Generated student should not be null");
        assertNotEquals(student1.getStudentId(), student2.getStudentId(),
            "Different student numbers should generate different IDs");
        assertTrue(student1.getExamScore() >= 50 && student1.getExamScore() <= 100,
            "Score should be between 50 and 100");
    }

    @Test
    public void testSubmitterWithDelay() throws InterruptedException, IOException {
        Student student = new Student("STU999", "Delayed Student", 88, "Chemistry");
        StudentScoreSubmitter submitter = new StudentScoreSubmitter(scoreLogger, student, 200);

        long startTime = System.currentTimeMillis();
        Thread thread = new Thread(submitter);
        thread.start();
        thread.join(5000);
        long endTime = System.currentTimeMillis();

        assertFalse(thread.isAlive(), "Thread should complete");
        assertTrue((endTime - startTime) >= 200,
            "Execution should take at least the specified delay");
        assertEquals(1, scoreLogger.getEntryCount(), "Score should be logged after delay");
    }

    @Test
    public void testConcurrentSubmissionIntegrity() throws InterruptedException, IOException {
        final int numThreads = 6;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            Student student = new Student("CONCURRENT" + i, "User" + i, 60 + i * 5, "Science");
            StudentScoreSubmitter submitter = new StudentScoreSubmitter(scoreLogger, student, 50);
            executor.submit(submitter);
        }

        executor.shutdown();
        boolean completed = executor.awaitTermination(15, TimeUnit.SECONDS);

        assertTrue(completed, "All concurrent submissions should complete");
        assertEquals(numThreads, scoreLogger.getEntryCount(),
            "All concurrent submissions should be logged correctly");

        // Verify file integrity
        java.util.List<String> lines = Files.readAllLines(Path.of(testFilePath));
        assertEquals(numThreads + 1, lines.size(),
            "File should have header plus entries for all threads");
    }
}
