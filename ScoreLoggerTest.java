package com.studentscores;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test cases for ScoreLogger
 * Tests file writing logic and thread safety
 */
public class ScoreLoggerTest {

    @TempDir
    Path tempDir;

    private ScoreLogger scoreLogger;
    private String testFilePath;

    @BeforeEach
    public void setUp() {
        testFilePath = tempDir.resolve("test_scores.csv").toString();
        scoreLogger = new ScoreLogger(testFilePath);
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Clean up test files
        Path path = Path.of(testFilePath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    @Test
    public void testFileInitialization() {
        assertTrue(Files.exists(Path.of(testFilePath)),
            "Log file should be created upon initialization");
    }

    @Test
    public void testLogSingleScore() throws IOException {
        Student student = new Student("STU001", "John Doe", 85, "Mathematics");
        scoreLogger.logScore(student);

        int count = scoreLogger.getEntryCount();
        assertEquals(1, count, "Should have exactly 1 entry after logging one score");
    }

    @Test
    public void testLogMultipleScores() throws IOException {
        Student student1 = new Student("STU001", "Alice Smith", 92, "Physics");
        Student student2 = new Student("STU002", "Bob Johnson", 78, "Chemistry");
        Student student3 = new Student("STU003", "Carol Williams", 88, "Biology");

        scoreLogger.logScore(student1);
        scoreLogger.logScore(student2);
        scoreLogger.logScore(student3);

        int count = scoreLogger.getEntryCount();
        assertEquals(3, count, "Should have exactly 3 entries after logging three scores");
    }

    @Test
    public void testClearLog() throws IOException {
        Student student = new Student("STU001", "Test Student", 95, "English");
        scoreLogger.logScore(student);

        assertEquals(1, scoreLogger.getEntryCount(), "Should have 1 entry before clearing");

        scoreLogger.clearLog();

        assertEquals(0, scoreLogger.getEntryCount(), "Should have 0 entries after clearing");
    }

    @Test
    public void testFileContentFormat() throws IOException {
        Student student = new Student("STU999", "Test User", 100, "History");
        scoreLogger.logScore(student);

        List<String> lines = Files.readAllLines(Path.of(testFilePath));
        assertTrue(lines.size() >= 2, "File should have header and at least one entry");

        String header = lines.get(0);
        assertTrue(header.contains("StudentID"), "Header should contain StudentID");
        assertTrue(header.contains("StudentName"), "Header should contain StudentName");
        assertTrue(header.contains("ExamScore"), "Header should contain ExamScore");

        String dataLine = lines.get(1);
        assertTrue(dataLine.contains("STU999"), "Data line should contain student ID");
        assertTrue(dataLine.contains("Test User"), "Data line should contain student name");
        assertTrue(dataLine.contains("100"), "Data line should contain score");
    }

    @Test
    public void testThreadSafetyWithTwoThreads() throws InterruptedException, IOException {
        final int numThreads = 2;
        final int scoresPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads * scoresPerThread);

        for (int i = 0; i < numThreads * scoresPerThread; i++) {
            final int studentNum = i;
            executor.submit(() -> {
                try {
                    Student student = new Student(
                        "STU" + String.format("%03d", studentNum),
                        "Student " + studentNum,
                        70 + (studentNum % 30),
                        "Subject" + (studentNum % 5)
                    );
                    scoreLogger.logScore(student);
                } catch (IOException e) {
                    fail("IOException during logging: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(completed, "All threads should complete within timeout");

        int expectedCount = numThreads * scoresPerThread;
        int actualCount = scoreLogger.getEntryCount();
        assertEquals(expectedCount, actualCount,
            "All scores should be logged without data loss due to race conditions");
    }

    @Test
    public void testThreadSafetyWithMultipleThreads() throws InterruptedException, IOException {
        final int numThreads = 5;
        final int scoresPerThread = 8;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads * scoresPerThread);

        for (int i = 0; i < numThreads * scoresPerThread; i++) {
            final int studentNum = i;
            executor.submit(() -> {
                try {
                    Student student = StudentScoreSubmitter.generateRandomStudent(studentNum);
                    scoreLogger.logScore(student);
                } catch (IOException e) {
                    fail("IOException during concurrent logging: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(completed, "All threads should complete within timeout");

        int expectedCount = numThreads * scoresPerThread;
        int actualCount = scoreLogger.getEntryCount();
        assertEquals(expectedCount, actualCount,
            "Thread safety should ensure no data loss with " + numThreads + " concurrent threads");
    }

    @Test
    public void testConcurrentWritesDataIntegrity() throws InterruptedException, IOException {
        final int numThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Student> studentsToLog = new ArrayList<>();

        // Create specific students to track
        for (int i = 0; i < numThreads; i++) {
            studentsToLog.add(new Student("ID" + i, "Name" + i, 80 + i, "Math"));
        }

        CountDownLatch latch = new CountDownLatch(numThreads);

        for (Student student : studentsToLog) {
            executor.submit(() -> {
                try {
                    scoreLogger.logScore(student);
                } catch (IOException e) {
                    fail("Failed to log score: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Verify all students were logged
        List<String> lines = Files.readAllLines(Path.of(testFilePath));
        assertEquals(numThreads + 1, lines.size(),
            "Should have header plus one line per student");

        // Verify each student ID appears in the file
        String fileContent = String.join("\n", lines);
        for (int i = 0; i < numThreads; i++) {
            assertTrue(fileContent.contains("ID" + i),
                "File should contain student ID" + i);
        }
    }

    @Test
    public void testGetFilePath() {
        assertEquals(testFilePath, scoreLogger.getFilePath(),
            "getFilePath should return the correct file path");
    }
}
