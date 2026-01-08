// Import packages
import java.io.*;
import java.util.*;

public class HighScoreManager {
    private static final String HIGH_SCORE_FILE = "highscores.txt";
    private static final int MAX_HIGH_SCORES = 5;
    
    // Load high scores from file
    public static List<Integer> loadHighScores() {
        List<Integer> highScores = new ArrayList<>();
        try {
            File file = new File(HIGH_SCORE_FILE);
            if (!file.exists()) {
                // Create file with default scores if it doesn't exist
                saveDefaultHighScores();
                return loadHighScores();
            }
            
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    int score = Integer.parseInt(line.trim());
                    highScores.add(score);
                } catch (NumberFormatException e) {
                    // Skip invalid lines
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Sort scores in descending order
        Collections.sort(highScores, Collections.reverseOrder());
        
        // Limit to max number of high scores
        while (highScores.size() > MAX_HIGH_SCORES) {
            highScores.remove(highScores.size() - 1);
        }
        
        return highScores;
    }
    
    // Save high scores to file
    public static void saveHighScores(List<Integer> highScores) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(HIGH_SCORE_FILE));
            for (Integer score : highScores) {
                writer.write(score.toString());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Add a new score and save updated high scores
    public static boolean addHighScore(int score) {
        List<Integer> highScores = loadHighScores();
        
        // Check if the score qualifies as a high score
        if (highScores.size() < MAX_HIGH_SCORES || score > highScores.get(highScores.size() - 1)) {
            highScores.add(score);
            Collections.sort(highScores, Collections.reverseOrder());
            
            // Limit to max number of high scores
            while (highScores.size() > MAX_HIGH_SCORES) {
                highScores.remove(highScores.size() - 1);
            }
            
            saveHighScores(highScores);
            return true;
        }
        
        return false;
    }
    
    // Create default high scores for a new file
    private static void saveDefaultHighScores() {
        List<Integer> defaultScores = new ArrayList<>();
        defaultScores.add(5000);
        defaultScores.add(4000);
        defaultScores.add(3000);
        defaultScores.add(2000);
        defaultScores.add(1000);
        saveHighScores(defaultScores);
    }
    
    // Get rank of the score (1-based)
    public static int getScoreRank(int score) {
        List<Integer> highScores = loadHighScores();
        for (int i = 0; i < highScores.size(); i++) {
            if (score >= highScores.get(i)) {
                return i + 1;
            }
        }
        return highScores.size() + 1;
    }
}
