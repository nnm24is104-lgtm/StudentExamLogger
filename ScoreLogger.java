
package com.studentscores;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thread-safe logger for writing student exam scores to a CSV file
 * Uses synchronized methods to ensure thread safety during file operations
 */
public class ScoreLogger {
    private final String filePath;
    private final Object writeLock = new Object();
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ScoreLogger(String filePath) {
        this.filePath = filePath;
        initializeFile();
    }

    /**
     * Initialize the CSV file with headers if it doesn't exist
     */
    private void initializeFile() {
        Path path = Paths.get(filePath);
        try {
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            if (!Files.exists(path)) {
                synchronized (writeLock) {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                        writer.write("StudentID,StudentName,ExamScore,Subject,Timestamp,ThreadName");
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error initializing file: " + e.getMessage());
        }
    }

    /**
     * Thread-safe method to log a student's exam score
     * Synchronized to prevent concurrent writes from corrupting the file
     */
    public synchronized void logScore(Student student) throws IOException {
        synchronized (writeLock) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                String timestamp = LocalDateTime.now().format(dateFormatter);
                String threadName = Thread.currentThread().getName();
                String logEntry = String.format("%s,%s,%d,%s,%s,%s",
                        student.getStudentId(),
                        student.getStudentName(),
                        student.getExamScore(),
                        student.getSubject(),
                        timestamp,
                        threadName);
                writer.write(logEntry);
                writer.newLine();
                writer.flush();
            }
        }
    }

    /**
     * Get the file path being used for logging
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Count the number of entries in the log file (excluding header)
     */
    public synchronized int getEntryCount() throws IOException {
        synchronized (writeLock) {
            long lineCount = Files.lines(Paths.get(filePath)).count();
            return (int) lineCount - 1; // Subtract header line
        }
    }

    /**
     * Clear all entries from the log file but keep the header
     */
    public synchronized void clearLog() throws IOException {
        synchronized (writeLock) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
                writer.write("StudentID,StudentName,ExamScore,Subject,Timestamp,ThreadName");
                writer.newLine();
            }
        }
    }
}
