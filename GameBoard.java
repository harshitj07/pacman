// Import packages
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

// Represents the main game board for the Pacman game.
public class GameBoard extends JPanel {
    private final Pacman pacman;
    private final Ghost[] ghosts;
    private int[][] maze;
    private final javax.swing.Timer timer; // javax.swing.Timer
    private JLabel scoreLabel;
    private int score = 0;
    private boolean[][] pellets;
    private int pelletsRemaining = 0;
    private int lives = 3;
    private final boolean[] powerPelletsActive = new boolean[4];
    private long powerPelletStartTime = 0;
    public static final int CELL_SIZE = 25;
    public static final int MAZE_WIDTH = 28;
    public static final int MAZE_HEIGHT = 31;
    private static final Color WALL_COLOR = new Color(20, 20, 150); // Darker blue for walls
    private static final Color DOT_COLOR = new Color(255, 255, 200); // Brighter yellow for pellets
    private static final Color POWER_PELLET_COLOR = new Color(255, 255, 255); // White for power pellets
    private static final Color BACKGROUND_COLOR = new Color(0, 0, 0); // Pure black background
    private static final long POWER_PELLET_DURATION = 10000; // 10 seconds
    private boolean gameOver = false;
    private boolean pacmanImmune = false;
    private long immunityStartTime = 0;
    private static final long IMMUNITY_DURATION = 1500; // 1.5 seconds of immunity after respawn
    private final int[][] powerPelletPositions = {
        {1, 3},    // Top-left region
        {26, 3},   // Top-right region
        {1, 23},   // Bottom-left region
        {26, 23}   // Bottom-right region
    };
    private java.util.Timer ghostTimer = new java.util.Timer();
    private final boolean[] ghostCanLeave = new boolean[4]; // Tracks which ghosts can leave the spawn area

    // Image for regular pellets
    private BufferedImage dotImage;

    // Fruit bonus that occasionally appears
    private BufferedImage fruitImage;
    private boolean fruitActive = false;
    private int fruitX, fruitY;
    private long fruitStartTime = 0;
    private static final long FRUIT_DURATION = 10000; // 10 seconds

    // Fruit notification variables
    private boolean showFruitNotification = false;
    private long fruitNotificationStartTime = 0;
    private static final long FRUIT_NOTIFICATION_DURATION = 2000; // 2 seconds

    private PacmanGame.StatsPanel statsPanel;

    // Track high score across game restarts
    private int highScore = 0;

    // High score variables
    private boolean isNewHighScore = false;
    private int currentScoreRank = 0;
    private List<Integer> highScores;

    // Track current level
    private int currentLevel = 1;

    // Track level transition
    private boolean inLevelTransition = false;
    private long levelTransitionStartTime = 0;
    private static final long LEVEL_TRANSITION_DURATION = 3000; // 3 seconds delay between levels 

    // Sets the statistics panel for displaying game stats.
    public void setStatsPanel(PacmanGame.StatsPanel statsPanel) {
        this.statsPanel = statsPanel;
        updateStats(); // Initialize stats
    }

    // Updates the statistics panel with current game values.
    private void updateStats() {
        if (statsPanel != null) {
            statsPanel.updateStats(score, lives, currentLevel);
        }
    }

    public GameBoard() {
        setBackground(BACKGROUND_COLOR);
        loadImages();
        initializeMaze();
        pacman = new Pacman(14, 23);
        ghosts = new Ghost[4];
        // Setup ghosts in the safe zone with appropriate colors (columns 11-17, rows 13-16)
        ghosts[0] = new Ghost(11, 13, new Color(200, 0, 0));      // Duller red
        ghosts[1] = new Ghost(16, 13, new Color(200, 150, 200));  // Duller pink
        ghosts[2] = new Ghost(11, 15, new Color(0, 200, 200));    // Duller cyan
        ghosts[3] = new Ghost(16, 15, new Color(200, 150, 50));   // Duller orange

        // Adjust the game speed to a better pace - make it a bit faster
        timer = new javax.swing.Timer(120, e -> { // Changed to 120ms for slightly faster gameplay
            update();
            repaint();
        });
        timer.start();

        scheduleInitialGhostReleases();
        initGame();
    }

    // Sets the difficulty level for the game and applies it to all ghosts.
    public void setDifficulty(int difficulty) {
        // Apply difficulty to all ghosts
        for (Ghost ghost : ghosts) {
            ghost.setDifficulty(difficulty);
            // Make sure each ghost has a reference to Pacman for tracking
            ghost.setPacman(pacman);
        }
        
        // Set consistent game speed across all difficulties
        timer.setDelay(120); // Use a consistent speed for all difficulty levels
    }

    // Loads game images from the file system.
    private void loadImages() {
        try {
            // Load dot image
            dotImage = ImageIO.read(new File("images/other/dot.png"));

            // Load fruit image (randomly select apple or strawberry)
            if (Math.random() < 0.5) {
                fruitImage = ImageIO.read(new File("images/other/apple.png"));
            } else {
                fruitImage = ImageIO.read(new File("images/other/strawberry.png"));
            }
        } catch (IOException e) {
            System.err.println("Error loading game images: " + e.getMessage());
            // Images will be null, game will fall back to simple shapes
        }
    }

    // Schedules the initial release of ghosts from their spawn area.
    private void scheduleInitialGhostReleases() {
        for (int i = 0; i < ghosts.length; i++) {
            final int ghostIndex = i;
            ghostTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    ghostCanLeave[ghostIndex] = true;
                    // Immediately move ghost outside the safe zone
                    moveGhostOutOfSafeZone(ghosts[ghostIndex]);
                }
            }, i * 5000); // Release each ghost at 5-second intervals
        }
    }
    
    // Helper method to move ghosts directly out of the safe zone.
    private void moveGhostOutOfSafeZone(Ghost ghost) {
        // Move ghost to row 11 (above the ghost box), column 14
        ghost.setPosition(14, 11); // Adjusted from row 12 to row 11
        try {
            java.lang.reflect.Method method = Ghost.class.getDeclaredMethod("setRandomDirection");
            method.setAccessible(true);
            method.invoke(ghost); // Call the private method using reflection
        } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            System.err.println("Error setting random direction for ghost: " + e.getMessage());
        }
    }

    // Schedules a ghost to respawn after being eaten by Pacman.
    private void scheduleGhostRespawn(int ghostIndex) {
        ghostCanLeave[ghostIndex] = false; // Prevent the ghost from leaving immediately
        ghostTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ghostCanLeave[ghostIndex] = true; // Allow the ghost to leave after 5 seconds
                // Immediately move ghost outside the safe zone
                moveGhostOutOfSafeZone(ghosts[ghostIndex]);
            }
        }, 5000); // 5-second delay for respawn
    }

    // Initializes the maze layout with wall and path definitions.
    private void initializeMaze() {
        maze = new int[][] {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,0,1},
            {1,0,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,0,1},
            {1,0,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,1,1,1,1,0,1,1,0,1,1,1,1,1,1,1,1,0,1,1,0,1,1,1,1,0,1},
            {1,0,1,1,1,1,0,1,1,0,1,1,1,1,1,1,1,1,0,1,1,0,1,1,1,1,0,1},
            {1,0,0,0,0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,1,1},
            {1,1,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,1,1},
            {1,1,1,1,1,1,0,1,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,1,1,1,1,1},
            {1,1,1,1,1,1,0,1,1,0,1,1,1,0,0,1,1,1,0,1,1,0,1,1,1,1,1,1},
            {1,1,1,1,1,1,0,1,1,0,1,0,0,0,0,0,0,1,0,1,1,0,1,1,1,1,1,1},
            {0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0},
            {1,1,1,1,1,1,0,1,1,0,1,0,0,0,0,0,0,1,0,1,1,0,1,1,1,1,1,1},
            {1,1,1,1,1,1,0,1,1,0,1,1,1,1,1,1,1,1,0,1,1,0,1,1,1,1,1,1},
            {1,1,1,1,1,1,0,1,1,0,0,0,0,0,0,0,0,0,0,1,1,0,1,1,1,1,1,1},
            {1,1,1,1,1,1,0,1,1,0,1,1,1,1,1,1,1,1,0,1,1,0,1,1,1,1,1,1},
            {1,1,1,1,1,1,0,1,1,0,1,1,1,1,1,1,1,1,0,1,1,0,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,0,1},
            {1,0,1,1,1,1,0,1,1,1,1,1,0,1,1,0,1,1,1,1,1,0,1,1,1,1,0,1},
            {1,0,0,0,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,0,0,0,1},
            {1,1,1,0,1,1,0,1,1,0,1,1,1,1,1,1,1,1,0,1,1,0,1,1,0,1,1,1},
            {1,1,1,0,1,1,0,1,1,0,1,1,1,1,1,1,1,1,0,1,1,0,1,1,0,1,1,1},
            {1,0,0,0,0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,1,1,0,0,0,0,0,0,1},
            {1,0,1,1,1,1,1,1,1,1,1,1,0,1,1,0,1,1,1,1,1,1,1,1,1,1,0,1},
            {1,0,1,1,1,1,1,1,1,1,1,1,0,1,1,0,1,1,1,1,1,1,1,1,1,1,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
        };
        setPreferredSize(new Dimension(MAZE_WIDTH * CELL_SIZE, MAZE_HEIGHT * CELL_SIZE));
    }

    // Initializes the game state including pellets and power pellets.
    public void initGame() {
        pellets = new boolean[maze.length][maze[0].length];
        pelletsRemaining = 0;

        // Define the correct safe zone for ghost spawn (columns 11-17, rows 13-16)
        int ghostBoxMinRow = 12;
        int ghostBoxMaxRow = 16;
        int ghostBoxMinCol = 11;
        int ghostBoxMaxCol = 17;

        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {
                if (maze[y][x] == 0) { // Check if it's a path
                    // Check if the current position is inside the ghost safe zone
                    boolean isGhostSpawnArea = (x >= ghostBoxMinCol && x <= ghostBoxMaxCol && 
                                                y >= ghostBoxMinRow && y <= ghostBoxMaxRow);

                    if (!isGhostSpawnArea) {
                        pellets[y][x] = true;
                        pelletsRemaining++;
                    } else {
                        pellets[y][x] = false; // No pellets in the ghost spawn area
                    }
                }
            }
        }

        for (int i = 0; i < powerPelletsActive.length; i++) {
            powerPelletsActive[i] = true;
            int x = powerPelletPositions[i][0];
            int y = powerPelletPositions[i][1];
            
            // Ensure power pellets are not placed in the safe zone
            boolean isPowerPelletInGhostBox = (x >= ghostBoxMinCol && x <= ghostBoxMaxCol && 
                                               y >= ghostBoxMinRow && y <= ghostBoxMaxRow);

            if (isPowerPelletInGhostBox) {
                powerPelletsActive[i] = false;
            } else if (x >= 0 && x < maze[0].length && y >= 0 && y < maze.length) {
                if (pellets[y][x]) {
                    pellets[y][x] = false;
                    pelletsRemaining--;
                }
            }
        }

        lives = 3;
        gameOver = false;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(MAZE_WIDTH * CELL_SIZE, MAZE_HEIGHT * CELL_SIZE);
    }

    // Sets the score label reference for UI updates.
    public void setScoreLabel(JLabel label) {
        this.scoreLabel = label;
        updateScore(score);
    }

    // Updates the player's score and refreshes the UI.
    public void updateScore(int points) {
        score += points;
        updateScoreLabel();
        updateStats();
    }

    // Updates the score label text with current score and lives.
    private void updateScoreLabel() {
        if (scoreLabel != null) {
            scoreLabel.setText("Score: " + score + " | Lives: " + lives);
        }
    }

    // Handles keyboard input for controlling Pacman and game actions.
    public void handleKeyPress(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP -> pacman.setDirection(0, -1);
            case KeyEvent.VK_DOWN -> pacman.setDirection(0, 1);
            case KeyEvent.VK_LEFT -> pacman.setDirection(-1, 0);
            case KeyEvent.VK_RIGHT -> pacman.setDirection(1, 0);
            case KeyEvent.VK_R -> {
                if (gameOver) {
                    restartGame();
                }
            }
        }
    }
    
    // Restarts the game to its initial state.
    private void restartGame() {
        // Reset game variables
        score = 0;
        currentLevel = 1;
        lives = 3;
        gameOver = false;
        inLevelTransition = false;
        isNewHighScore = false;
        
        // Cancel any existing ghost timer tasks
        ghostTimer.cancel();
        ghostTimer = new java.util.Timer();
        
        // Reset ghost leave flags
        for (int i = 0; i < ghostCanLeave.length; i++) {
            ghostCanLeave[i] = false;
        }
        
        // Reset Pacman to starting position
        pacman.reset();
        
        // Reset all ghosts to their starting positions
        for (Ghost ghost : ghosts) {
            ghost.reset();
        }
        
        // Reset power pellet state
        for (int i = 0; i < powerPelletsActive.length; i++) {
            powerPelletsActive[i] = true;
        }
        powerPelletStartTime = 0;
        
        // Reset fruit state
        fruitActive = false;
        fruitStartTime = 0;
        
        // Reset pellets
        initGame();
        
        // Update UI
        updateScoreLabel();
        updateStats();
        
        // Schedule ghost releases
        scheduleInitialGhostReleases();
        
        // Set brief immunity period for Pacman
        pacmanImmune = true;
        immunityStartTime = System.currentTimeMillis();
    }

    //  Main collision detection system for the game.
    private void checkCollisions() {
        int pacmanX = pacman.getX();
        int pacmanY = pacman.getY();

        // Check for pellet collection
        if (pacmanX >= 0 && pacmanX < maze[0].length && pacmanY >= 0 && pacmanY < maze.length) {
            if (pellets[pacmanY][pacmanX]) {
                pellets[pacmanY][pacmanX] = false;
                pelletsRemaining--;
                score += 10;
                updateScoreLabel();
                updateStats();

                if (pelletsRemaining <= 0) {
                    gameWon();
                }
            }
        }

        // Check for power pellet collection
        for (int i = 0; i < powerPelletsActive.length; i++) {
            int ppX = powerPelletPositions[i][0];
            int ppY = powerPelletPositions[i][1];

            if (powerPelletsActive[i] && pacmanX == ppX && pacmanY == ppY) {
                powerPelletsActive[i] = false;
                activatePowerPellet();
                score += 50;
                updateScoreLabel();
                updateStats();
            }
        }

        // Check for fruit collision
        checkFruitCollision();

        // Improved collision detection with ghosts
        // Check for ghost collisions with more precise detection
        for (Ghost ghost : ghosts) {
            if (pacmanX == ghost.getX() && pacmanY == ghost.getY()) {
                handleGhostCollision(ghost);
            }
            // Optional: Add a small tolerance for near-misses to improve collision detection
            else if (Math.abs(pacmanX - ghost.getX()) <= 0.5 && Math.abs(pacmanY - ghost.getY()) <= 0.5) {
                handleGhostCollision(ghost);
            }
        }
    }

    // Handles collision between Pacman and a ghost.
    private void handleGhostCollision(Ghost ghost) {
        if (pacmanImmune) return;

        if (ghost.isVulnerable()) {
            int ghostIndex = getGhostIndex(ghost);
            ghost.reset();
            scheduleGhostRespawn(ghostIndex); // Schedule respawn delay for the ghost
            score += 200;
            updateScoreLabel();
            updateStats();
        } else {
            lives--;
            updateScoreLabel();
            updateStats();

            if (lives <= 0) {
                gameOver();
            } else {
                resetAfterDeath();
            }
        }
    }

    // Finds the array index of a specific ghost.
    private int getGhostIndex(Ghost ghost) {
        for (int i = 0; i < ghosts.length; i++) {
            if (ghosts[i] == ghost) {
                return i;
            }
        }
        return -1; // Should never happen
    }

    // Resets game state after Pacman dies.
    private void resetAfterDeath() {
        pacman.reset();
        pacmanImmune = true;
        immunityStartTime = System.currentTimeMillis();

        // Reset all ghosts to their starting positions
        for (int i = 0; i < ghosts.length; i++) {
            ghosts[i].reset();
            ghostCanLeave[i] = false; // Make sure no ghost can leave initially
        }

        // Cancel any existing ghost timer tasks
        ghostTimer.cancel();
        // Create a new ghost timer
        ghostTimer = new java.util.Timer();
        
        // Schedule ghosts to be released at 5-second intervals, just like at the start
        scheduleInitialGhostReleases();
    }

    // Activates the power pellet effect.
    private void activatePowerPellet() {
        // Record the exact time when the power pellet is activated
        powerPelletStartTime = System.currentTimeMillis();
        
        // Make all ghosts vulnerable
        for (Ghost ghost : ghosts) {
            ghost.setVulnerable(true); // Ghosts become eatable
        }
    }

    // Deactivates the power pellet effect.
    private void deactivatePowerPellet() {
        // Reset all ghosts to their normal state
        for (Ghost ghost : ghosts) {
            ghost.setVulnerable(false); // Ghosts return to their usual state
        }
        // Reset the power pellet timer
        powerPelletStartTime = 0;
    }

    // Checks if the power pellet effect is currently active.
    private boolean isPowerPelletActive() {
        // Check if the power pellet effect is still active
        return (powerPelletStartTime > 0) && 
               (System.currentTimeMillis() - powerPelletStartTime < POWER_PELLET_DURATION);
    }

    // Handles game over state. Updates high scores and prepares game over screen.
    private void gameOver() {
        gameOver = true;
        // Keep track of the high score
        if (score > highScore) {
            highScore = score;
        }
        
        // Save high score to file and check ranking
        isNewHighScore = HighScoreManager.addHighScore(score);
        highScores = HighScoreManager.loadHighScores();
        currentScoreRank = HighScoreManager.getScoreRank(score);
    }

    // Handles level completion.
    private void gameWon() {
        // Start level transition instead of immediately advancing to next level
        currentLevel++;
        inLevelTransition = true;
        levelTransitionStartTime = System.currentTimeMillis();
        // Don't stop the timer - just let it continue running during transition
    }
    
    // Starts the next level after the transition period.
    private void startNextLevel() {
        // Reset the transition flag
        inLevelTransition = false;
        
        // Cancel any existing ghost timer tasks
        ghostTimer.cancel();
        ghostTimer = new java.util.Timer();
        
        // Reset positions of Pacman and ghosts
        pacman.reset();
        for (Ghost ghost : ghosts) {
            ghost.reset();
        }
        
        // Reset ghost leave flags
        for (int i = 0; i < ghostCanLeave.length; i++) {
            ghostCanLeave[i] = false;
        }
        
        // Refill the maze with pellets
        resetMazeForLevel();
        
        // Reset power pellets
        for (int i = 0; i < powerPelletsActive.length; i++) {
            powerPelletsActive[i] = true;
        }
        
        // Make Pacman immune briefly at the start of the level
        pacmanImmune = true;
        immunityStartTime = System.currentTimeMillis();
        
        // Schedule ghost releases
        scheduleInitialGhostReleases();
        
        // No need to restart the timer since we never stopped it
    }

    private void resetMazeForLevel() {
        // Reset pellets
        pellets = new boolean[maze.length][maze[0].length];
        pelletsRemaining = 0;
        
        // Define ghost spawn area
        int ghostBoxMinRow = 12;
        int ghostBoxMaxRow = 16;
        int ghostBoxMinCol = 11;
        int ghostBoxMaxCol = 17;
        
        // Place pellets in all valid locations
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {
                if (maze[y][x] == 0) { 
                    // Check if in ghost spawn area
                    boolean isGhostSpawnArea = (x >= ghostBoxMinCol && x <= ghostBoxMaxCol && 
                                                y >= ghostBoxMinRow && y <= ghostBoxMaxRow);
                    
                    if (!isGhostSpawnArea) {
                        pellets[y][x] = true;
                        pelletsRemaining++;
                    } else {
                        pellets[y][x] = false;
                    }
                }
            }
        }
        
        // Place power pellets and remove regular pellets at those positions
        for (int i = 0; i < powerPelletPositions.length; i++) {
            int x = powerPelletPositions[i][0];
            int y = powerPelletPositions[i][1];
            
            boolean isPowerPelletInGhostBox = (x >= ghostBoxMinCol && x <= ghostBoxMaxCol && 
                                             y >= ghostBoxMinRow && y <= ghostBoxMaxRow);
                                             
            if (isPowerPelletInGhostBox) {
                powerPelletsActive[i] = false;
            } else if (x >= 0 && x < maze[0].length && y >= 0 && y < maze.length) {
                if (pellets[y][x]) {
                    pellets[y][x] = false;
                    pelletsRemaining--;
                }
            }
        }
    }

    private void updateGhostMovements() {
        for (int i = 0; i < ghosts.length; i++) {
            if (ghostCanLeave[i]) {
                // Call the correct method with the appropriate number of parameters
                ghosts[i].move(maze);
            }
        }
    }

    // Spawns a fruit at a random position on the game board.
    private void spawnFruit() {
        // Find a random valid position for the fruit
        int attempts = 0;
        int maxAttempts = 100;
        
        while (attempts < maxAttempts) {
            int x = (int)(Math.random() * MAZE_WIDTH);
            int y = (int)(Math.random() * MAZE_HEIGHT);
            
            // Check if position is valid (not a wall and not in ghost spawn area)
            if (maze[y][x] == 0 && !isInGhostSpawnArea(x, y)) {
                fruitX = x;
                fruitY = y;
                fruitActive = true;
                fruitStartTime = System.currentTimeMillis();
                break;
            }
            attempts++;
        }
    }

    // Checks if the given coordinates are within the ghost spawn area.
    private boolean isInGhostSpawnArea(int x, int y) {
        return (x >= 11 && x <= 17 && y >= 12 && y <= 16);
    }

    // Checks for collision between Pacman and the fruit.
    private void checkFruitCollision() {
        if (fruitActive && pacman.getX() == fruitX && pacman.getY() == fruitY) {
            fruitActive = false;
            score += 100; // Bonus points for fruit
            updateScoreLabel();
            updateStats();
            
            // Trigger fruit notification
            showFruitNotification = true;
            fruitNotificationStartTime = System.currentTimeMillis();
        }
    }

    private void update() {
        // Check if we're in level transition and it's time to start the next level
        if (inLevelTransition) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - levelTransitionStartTime >= LEVEL_TRANSITION_DURATION) {
                startNextLevel();
            }
            repaint(); // Keep repainting during transition
            return;
        }
        
        if (gameOver) return;

        // Move Pacman first
        pacman.move(maze);
        
        // Check for collisions immediately after Pacman moves
        checkCollisions();
        
        // Update ghost movements
        updateGhostMovements();
        
        // Check for collisions again after ghosts move
        checkCollisions();
        
        // Check if the power pellet effect has expired
        if (isPowerPelletActive()) {
            long elapsedTime = System.currentTimeMillis() - powerPelletStartTime;
            if (elapsedTime >= POWER_PELLET_DURATION) {
                deactivatePowerPellet(); // Reset ghosts to their normal state
            }
        }

        // Handle fruit spawning and duration
        if (!fruitActive && Math.random() < 0.01) { // 1% chance per update to spawn fruit
            spawnFruit();
        }
        
        // Check if fruit should expire
        if (fruitActive && System.currentTimeMillis() - fruitStartTime > FRUIT_DURATION) {
            fruitActive = false;
        }

        // Check if fruit notification should expire
        if (showFruitNotification && System.currentTimeMillis() - fruitNotificationStartTime > FRUIT_NOTIFICATION_DURATION) {
            showFruitNotification = false;
        }

        // Check immunity status
        if (pacmanImmune && System.currentTimeMillis() - immunityStartTime > IMMUNITY_DURATION) {
            pacmanImmune = false;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the maze walls without any rounded corners
        for (int i = 0; i < MAZE_HEIGHT; i++) {
            for (int j = 0; j < MAZE_WIDTH; j++) {
                if (maze[i][j] == 1) {
                    g2d.setColor(WALL_COLOR);
                    // Use regular rectangles for all walls
                    g2d.fillRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        // Draw the pellets
        for (int y = 0; y < pellets.length; y++) {
            for (int x = 0; x < pellets[0].length; x++) {
                if (pellets[y][x]) {
                    if (dotImage != null) {
                        g2d.drawImage(dotImage, x * CELL_SIZE + CELL_SIZE / 4, y * CELL_SIZE + CELL_SIZE / 4, CELL_SIZE / 2, CELL_SIZE / 2, null);
                    } else {
                        g2d.setColor(DOT_COLOR);
                        g2d.fillOval(x * CELL_SIZE + CELL_SIZE * 3 / 8, y * CELL_SIZE + CELL_SIZE * 3 / 8, CELL_SIZE / 4, CELL_SIZE / 4);
                    }
                }
            }
        }

        // Draw the power pellets
        for (int i = 0; i < powerPelletPositions.length; i++) {
            if (powerPelletsActive[i]) {
                int x = powerPelletPositions[i][0];
                int y = powerPelletPositions[i][1];
                g2d.setColor(POWER_PELLET_COLOR);
                // Larger dots for power pellets
                g2d.fillOval(x * CELL_SIZE + CELL_SIZE / 4, y * CELL_SIZE + CELL_SIZE / 4, CELL_SIZE / 2, CELL_SIZE / 2);
            }
        }

        // Draw Pacman
        pacman.draw(g2d, CELL_SIZE);

        // Draw the ghosts
        for (Ghost ghost : ghosts) {
            ghost.draw(g2d, CELL_SIZE);
        }

        // Draw the fruit if active
        if (fruitActive) {
            if (fruitImage != null) {
                g2d.drawImage(fruitImage, fruitX * CELL_SIZE, fruitY * CELL_SIZE, CELL_SIZE, CELL_SIZE, null);
            } else {
                // Fallback to simple fruit representation
                g2d.setColor(Color.RED);
                g2d.fillOval(fruitX * CELL_SIZE + CELL_SIZE / 4, fruitY * CELL_SIZE + CELL_SIZE / 4, 
                           CELL_SIZE / 2, CELL_SIZE / 2);
            }
        }

        // Optional: Add visual indicator for power pellet timer
        if (isPowerPelletActive()) {
            // Calculate remaining time as a percentage
            double remainingTime = 1.0 - ((double)(System.currentTimeMillis() - powerPelletStartTime) / POWER_PELLET_DURATION);
            
            // Draw a timer bar at the top of the screen
            g2d.setColor(Color.BLUE);
            int timerWidth = (int)(getWidth() * remainingTime);
            g2d.fillRect(0, 0, timerWidth, 5);
            
            // Draw the countdown text
            int secondsLeft = (int)Math.ceil((POWER_PELLET_DURATION - (System.currentTimeMillis() - powerPelletStartTime)) / 1000.0);
            g2d.setColor(Color.WHITE);
            g2d.setFont(FontManager.RETRO_FONT.deriveFont(16f));
            String timerText = "POWER TIME: " + secondsLeft;
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(timerText);
            g2d.drawString(timerText, (getWidth() - textWidth) / 2, 20);
        }

        // Draw fruit consumption notification
        if (showFruitNotification) {
            // Calculate fade effect based on remaining time
            long elapsed = System.currentTimeMillis() - fruitNotificationStartTime;
            float alpha = 1.0f - (float)elapsed / FRUIT_NOTIFICATION_DURATION;
            alpha = Math.max(0, Math.min(1, alpha)); // Clamp between 0 and 1
            
            // Create pulsing effect
            float pulseScale = 1.0f + 0.3f * (float)Math.sin(elapsed * 0.01);
            
            g2d.setColor(new Color(255, 255, 0, (int)(255 * alpha))); // Yellow with fade
            Font fruitFont = FontManager.RETRO_FONT.deriveFont(18f * pulseScale);
            g2d.setFont(fruitFont);
            
            String fruitText = "+100 FRUIT BONUS!";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(fruitText);
            int x = (getWidth() - textWidth) / 2;
            int y = isPowerPelletActive() ? 45 : 25; // Position below power pellet timer if active
            
            g2d.drawString(fruitText, x, y);
        }

        // Draw level transition screen
        if (inLevelTransition) {
            // Create a black overlay with a slight fade effect
            g2d.setColor(new Color(0, 0, 0, 230)); // Semi-transparent black overlay
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            // Calculate progress through transition (0.0 to 1.0)
            float progress = Math.min(1.0f, (float)(System.currentTimeMillis() - levelTransitionStartTime) / LEVEL_TRANSITION_DURATION);
            
            // Draw level title with a pulse effect
            float pulseScale = 1.0f + 0.2f * (float)Math.sin(progress * Math.PI * 4);
            
            g2d.setColor(Color.YELLOW);
            Font levelFont = FontManager.RETRO_FONT.deriveFont(40f * pulseScale);
            g2d.setFont(levelFont);
            
            String levelText = "LEVEL " + currentLevel;
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(levelText);
            int x = (getWidth() - textWidth) / 2;
            int y = getHeight() / 2;
            
            g2d.drawString(levelText, x, y);
            
            // Draw "GET READY!" text below the level number
            g2d.setFont(FontManager.RETRO_FONT.deriveFont(16f));
            String readyText = "GET READY!";
            fm = g2d.getFontMetrics();
            textWidth = fm.stringWidth(readyText);
            x = (getWidth() - textWidth) / 2;
            
            // Make the ready text appear after a slight delay
            if (progress > 0.3f) {
                g2d.setColor(new Color(255, 255, 255, 
                    (int)(255 * Math.min(1.0f, (progress - 0.3f) / 0.7f))));
                g2d.drawString(readyText, x, y + 50);
            }
        }

        // Draw the game over or win screen if the game is over
        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 180)); // Semi-transparent black overlay
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Top section with game over message
            g2d.setColor(lives <= 0 ? Color.RED : Color.YELLOW);
            g2d.setFont(FontManager.RETRO_FONT.deriveFont(28f));
            String message = lives <= 0 ? "GAME OVER" : "YOU WIN!";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(message);
            int x = (getWidth() - textWidth) / 2;
            int y = 100;
            g2d.drawString(message, x, y);

            // Final score display
            g2d.setFont(FontManager.RETRO_FONT.deriveFont(20f));
            g2d.setColor(Color.WHITE);
            String finalScoreText = "YOUR SCORE: " + score;
            textWidth = g2d.getFontMetrics().stringWidth(finalScoreText);
            g2d.drawString(finalScoreText, (getWidth() - textWidth) / 2, y + 40);
            
            // New high score notification if applicable
            if (isNewHighScore) {
                g2d.setFont(FontManager.RETRO_FONT.deriveFont(16f));
                g2d.setColor(Color.YELLOW);
                String newHighScoreText = "NEW HIGH SCORE!";
                textWidth = g2d.getFontMetrics().stringWidth(newHighScoreText);
                g2d.drawString(newHighScoreText, (getWidth() - textWidth) / 2, y + 70);
                
                String rankText = "RANK: #" + currentScoreRank;
                textWidth = g2d.getFontMetrics().stringWidth(rankText);
                g2d.drawString(rankText, (getWidth() - textWidth) / 2, y + 95);
            }
            
            // Draw high score podium
            drawHighScorePodium(g2d, y + 130);
            
            // Restart instruction at the bottom
            g2d.setColor(Color.WHITE);
            g2d.setFont(FontManager.RETRO_FONT.deriveFont(12f));
            String restartText = "PRESS R TO RESTART";
            textWidth = g2d.getFontMetrics().stringWidth(restartText);
            g2d.drawString(restartText, (getWidth() - textWidth) / 2, getHeight() - 50);
        }
    }
    
    // Draw the high score podium showing top 5 scores
    private void drawHighScorePodium(Graphics2D g2d, int startY) {
        // Title for high scores section
        g2d.setColor(Color.CYAN);
        g2d.setFont(FontManager.RETRO_FONT.deriveFont(18f));
        String highScoreTitle = "HIGH SCORES";
        int textWidth = g2d.getFontMetrics().stringWidth(highScoreTitle);
        g2d.drawString(highScoreTitle, (getWidth() - textWidth) / 2, startY);
        
        // Draw podium positions
        if (highScores != null && !highScores.isEmpty()) {
            // Set up dimensions
            int podiumWidth = 80;
            int podiumSpacing = 20;
            int topPodiumHeight = 120;
            int secondPodiumHeight = 100;
            int thirdPodiumHeight = 80;
            
            int centerX = getWidth() / 2;
            int podiumBaseY = startY + 180; // Bottom of the podiums
            
            // Draw podium blocks for top 3 positions
            if (highScores.size() >= 3) {
                // First place (center)
                g2d.setColor(new Color(255, 215, 0)); // Gold
                g2d.fillRect(centerX - podiumWidth/2, podiumBaseY - topPodiumHeight, podiumWidth, topPodiumHeight);
                
                // Second place (left)
                g2d.setColor(new Color(192, 192, 192)); // Silver
                g2d.fillRect(centerX - podiumWidth*3/2 - podiumSpacing, podiumBaseY - secondPodiumHeight, podiumWidth, secondPodiumHeight);
                
                // Third place (right)
                g2d.setColor(new Color(205, 127, 50)); // Bronze
                g2d.fillRect(centerX + podiumWidth/2 + podiumSpacing, podiumBaseY - thirdPodiumHeight, podiumWidth, thirdPodiumHeight);
                
                // Draw rank numbers and scores on podiums
                g2d.setFont(FontManager.RETRO_FONT.deriveFont(16f));
                
                // First place
                g2d.setColor(Color.BLACK);
                drawCenteredString(g2d, "1", centerX, podiumBaseY - topPodiumHeight + 25);
                drawCenteredString(g2d, String.valueOf(highScores.get(0)), centerX, podiumBaseY - topPodiumHeight/2);
                
                // Second place
                drawCenteredString(g2d, "2", centerX - podiumWidth - podiumSpacing, podiumBaseY - secondPodiumHeight + 25);
                drawCenteredString(g2d, String.valueOf(highScores.get(1)), centerX - podiumWidth - podiumSpacing, podiumBaseY - secondPodiumHeight/2);
                
                // Third place
                drawCenteredString(g2d, "3", centerX + podiumWidth + podiumSpacing, podiumBaseY - thirdPodiumHeight + 25);
                drawCenteredString(g2d, String.valueOf(highScores.get(2)), centerX + podiumWidth + podiumSpacing, podiumBaseY - thirdPodiumHeight/2);
            }
            
            // Draw 4th and 5th places below the podium
            g2d.setColor(Color.WHITE);
            g2d.setFont(FontManager.RETRO_FONT.deriveFont(14f));
            
            if (highScores.size() >= 4) {
                g2d.drawString("4. " + highScores.get(3), centerX - 100, podiumBaseY + 30);
            }
            
            if (highScores.size() >= 5) {
                g2d.drawString("5. " + highScores.get(4), centerX + 20, podiumBaseY + 30);
            }
            
            // If current score is on the list but not in top 3, highlight it
            if (currentScoreRank >= 4 && currentScoreRank <= highScores.size()) {
                g2d.setColor(Color.YELLOW);
                String rankText = currentScoreRank + ". " + score;
                g2d.drawString(rankText, 
                    currentScoreRank == 4 ? centerX - 100 : centerX + 20, 
                    podiumBaseY + 30);
            }
        }
    }
    
    // Helper method to draw centered text
    private void drawCenteredString(Graphics2D g2d, String text, int centerX, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g2d.drawString(text, centerX - textWidth/2, y);
    }
}
