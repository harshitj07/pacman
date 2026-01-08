// Import packages
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;

/**
 * Ghost class represents AI-controlled enemies in the Pacman game.
 * Each ghost has unique behavior patterns based on classic Pacman ghost personalities:
 * - Blinky (Red): Direct aggressive chaser
 * - Pinky (Pink): Ambusher who targets ahead of Pacman
 * - Inky (Cyan): Uses teamwork strategy with Blinky
 * - Clyde (Orange): Unpredictable behavior based on distance
 */
public class Ghost {
    // Position coordinates on the game grid
    private int x, y;
    
    // Current movement direction (-1, 0, or 1 for each axis)
    private int dx, dy;
    
    // Visual properties
    private final Color color;
    
    // AI behavior timing
    private int directionChangeCounter;
    private static final int DIRECTION_CHANGE_INTERVAL = 20;
    
    // Sprite images for rendering
    private BufferedImage ghostImage;
    private static BufferedImage blueGhostImage; // Shared vulnerable state image
    
    // Vulnerability state (when Pacman eats power pellet)
    private boolean isVulnerable = false;
    private Timer vulnerabilityTimer;
    
    // Game world constants
    private static final int MAZE_WIDTH = 28;
    
    // Respawn position tracking
    private final int startX;
    private final int startY;
    
    // Safe zone management (prevents ghosts from re-entering spawn area)
    private boolean hasLeftSafeZone = false;
    
    // Ghost personality system for different AI behaviors
    private final int personalityType;
    // 0: Chaser - Aggressively chases Pacman (Blinky - Red)
    // 1: Ambusher - Tries to cut off Pacman's path (Pinky - Pink)
    // 2: Patrol - Patrols between Pacman and fixed position (Inky - Cyan)
    // 3: Random - More random movement (Clyde - Orange)
    
    // Difficulty scaling system
    private int difficulty = 1; // 1=Easy, 2=Medium, 3=Hard
    private final Random random = new Random();
    
    // Ghost coordination system - tracks all ghost positions for collision avoidance
    private static final ArrayList<int[]> GHOST_POSITIONS = new ArrayList<>();
    
    // Reference to Pacman for AI targeting calculations
    private Pacman pacmanRef;

    // Creates a ghost with specified position and color.
    public Ghost(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.startX = x; // Remember spawn position for respawning
        this.startY = y;
        this.color = color;
        this.dx = 0; // Start stationary
        this.dy = 0;
        this.directionChangeCounter = 0;
        
        // Assign personality based on classic Pacman ghost colors
        if (color.equals(new Color(200, 0, 0))) {  // Red (Blinky)
            personalityType = 0; // Aggressive chaser
        } else if (color.equals(new Color(200, 150, 200))) {  // Pink (Pinky)
            personalityType = 1; // Ambusher
        } else if (color.equals(new Color(0, 200, 200))) {  // Cyan (Inky)
            personalityType = 2; // Team player
        } else if (color.equals(new Color(200, 150, 50))) {  // Orange (Clyde)
            personalityType = 3; // Unpredictable
        } else {
            personalityType = 3; // Default to random behavior for unknown colors
        }
        
        loadImage();
        setRandomDirection();
        
        // Initialize this ghost's position in the shared tracking system
        while (GHOST_POSITIONS.size() <= personalityType) {
            GHOST_POSITIONS.add(new int[]{0, 0});
        }
        GHOST_POSITIONS.set(personalityType, new int[]{x, y});
    }

    // Current X coordinate on the game grid
    public int getX() {
        return x;
    }

    // Current Y coordinate on the game grid
    public int getY() {
        return y;
    }

    // True if ghost is currently vulnerable (blue state)
    public boolean isVulnerable() {
        return isVulnerable;
    }

    // Loads sprite images for the ghost based on its color/personality. Each ghost type has a unique sprite, plus a shared blue vulnerable sprite.
    private void loadImage() {
        try {
            // Load the shared blue ghost image for vulnerable state (loaded once)
            if (blueGhostImage == null) {
                blueGhostImage = ImageIO.read(new File("images/ghosts/blue_ghost.png"));
            }
            
            // Load the specific ghost sprite based on personality/color
            if (color.equals(new Color(200, 0, 0))) {  // Red ghost (Blinky)
                ghostImage = ImageIO.read(new File("images/ghosts/blinky.png"));
            } else if (color.equals(new Color(200, 150, 200))) {  // Pink ghost (Pinky)
                ghostImage = ImageIO.read(new File("images/ghosts/pinky.png"));
            } else if (color.equals(new Color(0, 200, 200))) {  // Cyan ghost (Inky)
                ghostImage = ImageIO.read(new File("images/ghosts/inky.png"));
            } else if (color.equals(new Color(200, 150, 50))) {  // Orange ghost (Clyde)
                ghostImage = ImageIO.read(new File("images/ghosts/clyde.png"));
            }
        } catch (IOException e) {
            System.err.println("Error loading ghost images: " + e.getMessage());
            // Will fall back to programmatic drawing if images fail to load
        }
    }

    // Sets a random movement direction for the ghost.
    private void setRandomDirection() {
        int direction = (int)(Math.random() * 4);
        switch (direction) {
            case 0 -> { dx = 1; dy = 0; }  // Right
            case 1 -> { dx = -1; dy = 0; } // Left
            case 2 -> { dx = 0; dy = 1; }  // Down
            case 3 -> { dx = 0; dy = -1; } // Up
        }
    }

    // Sets the difficulty level which affects ghost intelligence and responsiveness.
    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
        
        // Adjust AI update frequency based on difficulty
        if (difficulty == 3) { // Hard mode
            // More frequent direction changes for aggressive behavior
            directionChangeCounter = DIRECTION_CHANGE_INTERVAL / 2;
        } else if (difficulty == 2) { // Medium mode
            // Moderately responsive AI
            directionChangeCounter = (int)(DIRECTION_CHANGE_INTERVAL / 1.5);
        }
        // Easy mode uses default timing
    }

    // Main movement method that handles AI decision making and position updates.
    public void move(int[][] maze) {
        // Determine AI update frequency based on difficulty level
        int updateFrequency;
        switch(difficulty) {
            case 3: // Hard mode - most responsive
                updateFrequency = 10;
                break;
            case 2: // Medium mode - moderately responsive
                updateFrequency = 15;
                break;
            default: // Easy mode - default pace
                updateFrequency = DIRECTION_CHANGE_INTERVAL;
        }
        
        // AI decision making timer
        directionChangeCounter++;
        if (directionChangeCounter >= updateFrequency) {
            directionChangeCounter = 0; // Reset counter
            
            // Choose behavior strategy based on difficulty
            if (difficulty == 3) { 
                // Hard mode: Always use intelligent behavior
                setDirectionBasedOnPersonality(maze);
            } else {
                // Easy/Medium modes: Mix of intelligent and random behavior
                double intelligenceLevel;
                switch (difficulty) {
                    case 1 -> intelligenceLevel = 0.2;  // Easy: 20% intelligent moves
                    case 2 -> intelligenceLevel = 0.65; // Medium: 65% intelligent moves
                    default -> intelligenceLevel = 0.3; // Fallback
                }

                // Randomly decide whether to use intelligent or random behavior
                if (random.nextDouble() < intelligenceLevel) {
                    setDirectionBasedOnPersonality(maze);
                } else {
                    setRandomDirection();
                }
            }
        }
        
        // Medium difficulty: Additional intersection intelligence
        if (difficulty == 2 && !isVulnerable && directionChangeCounter % 5 == 0) {
            checkIntersectionDecision(maze);
        }

        // Calculate next position based on current direction
        int newX = x + dx;
        int newY = y + dy;

        // Handle tunnel teleportation at row 14 (classic Pacman feature)
        if (newX < 0 && y == 14) { // Exiting left side
            newX = MAZE_WIDTH - 1; // Teleport to right side
        } else if (newX >= MAZE_WIDTH && y == 14) { // Exiting right side
            newX = 0; // Teleport to left side
        }

        // Ghost collision detection - prevent ghosts from occupying same space
        boolean ghostCollision = false;
        for (int[] pos : GHOST_POSITIONS) {
            // Check if another ghost is at the target position
            if (pos[0] == newX && pos[1] == newY && !(pos[0] == x && pos[1] == y)) {
                ghostCollision = true;
                break;
            }
        }

        // Execute movement if valid
        if (isValidMove(newX, newY, maze) && !ghostCollision) {
            x = newX;
            y = newY;
            updateGhostPosition(); // Update shared position tracking
            
            // Hard mode: Additional intersection checking for optimal pathfinding
            if (difficulty == 3 && !isVulnerable) {
                checkIntersectionDecision(maze);
            }
        } else {
            // If movement blocked, find alternative direction
            findAlternativeDirection(maze);
        }
    }
    
    // Analyzes intersections and makes intelligent pathfinding decisions.
    private void checkIntersectionDecision(int[][] maze) {
        // Count available directions (excluding reverse direction to avoid back-and-forth)
        int possibleDirs = 0;
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}; // up, right, down, left
        int oppositeX = -dx; // Reverse of current horizontal direction
        int oppositeY = -dy; // Reverse of current vertical direction
        
        // Evaluate each possible direction
        for (int[] dir : dirs) {
            // Skip the direction we just came from
            if (dir[0] == oppositeX && dir[1] == oppositeY) continue;
            
            // Count valid movement options
            if (isValidMove(x + dir[0], y + dir[1], maze)) {
                possibleDirs++;
            }
        }
        
        // If at an intersection (multiple valid directions), reconsider strategy
        if (possibleDirs > 1) {
            setDirectionBasedOnPersonality(maze);
        }
    }
    
    // Core AI behavior system that implements different ghost personalities.
    private void setDirectionBasedOnPersonality(int[][] maze) {
        // When vulnerable, all ghosts flee from Pacman regardless of personality
        if (isVulnerable) {
            if (pacmanRef != null) {
                fleeFromPacman(pacmanRef.getX(), pacmanRef.getY());
            } else {
                setRandomDirection(); // Fallback if no Pacman reference
            }
            return;
        }
        
        // Require Pacman reference for intelligent targeting
        if (pacmanRef == null) {
            setRandomDirection();
            return;
        }
        
        // Get Pacman's current state for targeting calculations
        int pacmanX = pacmanRef.getX();
        int pacmanY = pacmanRef.getY();
        int pacmanDirX = pacmanRef.getDirectionX();
        int pacmanDirY = pacmanRef.getDirectionY();
        
        // Execute personality-specific behavior
        switch (personalityType) {
            case 0: // Blinky (Red) - Aggressive direct chaser
                // Always targets Pacman's exact current position
                directChase(pacmanX, pacmanY);
                
                // Extra aggression in hard mode
                if (difficulty == 3) {
                    directionChangeCounter = -2; // Update more frequently
                }
                break;
                
            case 1: // Pinky (Pink) - Ambusher
                // Targets 4 tiles ahead of Pacman's current direction
                int targetX = pacmanX + (pacmanDirX * 4);
                int targetY = pacmanY + (pacmanDirY * 4);
                
                // Reproduce original Pacman bug: when moving up, also offset left
                if (pacmanDirX == 0 && pacmanDirY == -1) {
                    targetX -= 4; // Historical game quirk
                }
                
                directChase(targetX, targetY);
                break;
                
            case 2: // Inky (Cyan) - Team player with Blinky
                // Complex targeting system using Blinky's position
                int intermediateX = pacmanX + (pacmanDirX * 2);
                int intermediateY = pacmanY + (pacmanDirY * 2);
                
                // Apply the same historical bug as Pinky
                if (pacmanDirX == 0 && pacmanDirY == -1) {
                    intermediateX -= 2;
                }
                
                // Find Blinky's current position for teamwork calculation
                int blinkyX = x; // Fallback to own position
                int blinkyY = y;
                if (GHOST_POSITIONS.size() > 0) {
                    int[] blinkyPos = GHOST_POSITIONS.get(0); // Blinky is index 0
                    if (blinkyPos[0] != 0 || blinkyPos[1] != 0) {
                        blinkyX = blinkyPos[0];
                        blinkyY = blinkyPos[1];
                    }
                }
                
                // Create vector from Blinky to intermediate target and extend it
                int vectorX = intermediateX - blinkyX;
                int vectorY = intermediateY - blinkyY;
                int targetInkyX = blinkyX + (vectorX * 2);
                int targetInkyY = blinkyY + (vectorY * 2);
                
                directChase(targetInkyX, targetInkyY);
                break;
                
            case 3: // Clyde (Orange) - Distance-based behavior
                // Calculate Manhattan distance to Pacman
                int distance = Math.abs(x - pacmanX) + Math.abs(y - pacmanY);
                
                if (distance > 8) {
                    // When far from Pacman, act aggressively (chase)
                    directChase(pacmanX, pacmanY);
                } else {
                    // When close to Pacman, retreat to corner (cowardly behavior)
                    directChase(1, 29); // Bottom-left corner of maze
                }
                break;
                
            default:
                // Fallback for unknown personality types
                setRandomDirection();
                break;
        }
    }
    
    // Calculates and sets direction toward a specific target coordinate.
    private void directChase(int targetX, int targetY) {
        // Calculate distance to target in each axis
        int deltaX = targetX - x;
        int deltaY = targetY - y;
        
        // Choose direction based on which axis has greater distance
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            // Horizontal distance is greater - move horizontally
            dx = Integer.compare(deltaX, 0); // Returns -1, 0, or 1
            dy = 0;
        } else {
            // Vertical distance is greater or equal - move vertically
            dx = 0;
            dy = Integer.compare(deltaY, 0);
        }
    }
    
    // Calculates movement direction to flee away from Pacman.
    private void fleeFromPacman(int pacmanX, int pacmanY) {
        // Calculate direction away from Pacman (opposite of chase logic)
        int deltaX = pacmanX - x;
        int deltaY = pacmanY - y;
        
        // Move in opposite direction of Pacman
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            // Flee horizontally
            dx = -Integer.compare(deltaX, 0); // Negate to flee instead of chase
            dy = 0;
        } else {
            // Flee vertically
            dx = 0;
            dy = -Integer.compare(deltaY, 0);
        }
    }
    
    // Finds an alternative valid movement direction when current path is blocked.
    private void findAlternativeDirection(int[][] maze) {
        // All possible movement directions
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}; // up, right, down, left
        
        // Calculate opposite direction to avoid immediate reversal
        int oppositeX = -dx;
        int oppositeY = -dy;
        
        // Try each direction until finding a valid one
        for (int[] dir : dirs) {
            // Skip the direction we just came from (prevents oscillation)
            if (dir[0] == oppositeX && dir[1] == oppositeY) continue;
            
            // Use first valid alternative direction found
            if (isValidMove(x + dir[0], y + dir[1], maze)) {
                dx = dir[0];
                dy = dir[1];
                break;
            }
        }
    }
    
    // Updates this ghost's position in the shared position tracking system.
    private void updateGhostPosition() {
        // Ensure the position array is large enough for this ghost
        while (GHOST_POSITIONS.size() <= personalityType) {
            GHOST_POSITIONS.add(new int[]{0, 0});
        }
        
        // Update this ghost's position in the shared array
        GHOST_POSITIONS.set(personalityType, new int[]{x, y});
    }

    // Validates whether a move to the specified coordinates is legal.
    private boolean isValidMove(int newX, int newY, int[][] maze) {
        // Special case: allow tunnel movement at row 14
        if ((newX < 0 || newX >= maze[0].length) && y == 14) {
            return true; // Tunnel positions are always valid
        }
        
        // Check basic boundary conditions
        if (newX < 0 || newX >= maze[0].length || newY < 0 || newY >= maze.length) {
            return false;
        }
        
        // Prevent ghosts from re-entering safe zone after leaving
        if (hasLeftSafeZone) {
            // Define the ghost safe zone (spawn area in center of maze)
            boolean isMovingToSafeZone = (newY >= 12 && newY <= 16 && newX >= 11 && newX <= 17);
            if (isMovingToSafeZone) {
                return false; // Block re-entry to safe zone
            }
        }
        
        // Check for wall collision (1 = wall, 0 = empty space)
        return maze[newY][newX] != 1;
    }

    // Sets the ghost's vulnerable state (blue ghost mode).
    public void setVulnerable(boolean vulnerable) {
        this.isVulnerable = vulnerable;
        
        if (vulnerable) {
            // Change direction when becoming vulnerable (classic behavior)
            setRandomDirection();
            
            // Cancel any existing vulnerability timer
            if (vulnerabilityTimer != null) {
                vulnerabilityTimer.cancel();
            }
            
            // Start 10-second countdown to return to normal
            vulnerabilityTimer = new Timer();
            vulnerabilityTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    setVulnerable(false); // Auto-return to normal state
                }
            }, 10000); // 10 seconds
        } else {
            // Cancel timer when manually returned to normal
            if (vulnerabilityTimer != null) {
                vulnerabilityTimer.cancel();
                vulnerabilityTimer = null;
            }
        }
    }

    // Respawns the ghost at its starting position with random direction.
    public void respawn() {
        x = startX;
        y = startY;
        dx = 0;
        dy = 0;
        setRandomDirection();
    }

    // Resets ghost to initial game state.
    public void reset() {
        x = startX;
        y = startY;
        dx = 0;
        dy = 0;
        directionChangeCounter = 0;
        setRandomDirection();
        isVulnerable = false;
        hasLeftSafeZone = false; // Reset safe zone tracking
    }

    // Manually sets the ghost's position on the game grid.
    public void setPosition(int newX, int newY) {
        this.x = newX;
        this.y = newY;
        
        // Mark ghost as having left safe zone when positioned at row 11
        // (This is typically the exit row from the ghost spawn area)
        if (newY == 11) {
            hasLeftSafeZone = true;
        }
    }

    // Renders the ghost on screen using either sprite images or fallback graphics.
    public void draw(Graphics2D g2d, int cellSize) {
        if (ghostImage != null) {
            // Use sprite images if successfully loaded
            BufferedImage imageToDraw = isVulnerable ? blueGhostImage : ghostImage;
            g2d.drawImage(imageToDraw, x * cellSize, y * cellSize, cellSize, cellSize, null);
        } else {
            // Fallback to programmatic drawing if images failed to load
            
            // Set color based on vulnerability state
            g2d.setColor(isVulnerable ? Color.BLUE : color);
            
            // Draw main ghost body (rounded rectangle)
            g2d.fillRoundRect(x * cellSize, y * cellSize, cellSize, cellSize, cellSize / 2, cellSize / 2);
            
            // Draw characteristic ghost bottom (wavy edge)
            int bottomY = y * cellSize + cellSize / 2;
            for (int i = 0; i < 3; i++) {
                g2d.fillArc(x * cellSize + i * cellSize / 3, bottomY, cellSize / 3, cellSize / 2, 0, 180);
            }
            
            // Draw ghost eyes (white circles)
            g2d.setColor(Color.WHITE);
            g2d.fillOval(x * cellSize + cellSize / 4, y * cellSize + cellSize / 4, cellSize / 4, cellSize / 4);
            g2d.fillOval(x * cellSize + cellSize / 2, y * cellSize + cellSize / 4, cellSize / 4, cellSize / 4);
            
            // Draw eye pupils (blue dots)
            g2d.setColor(Color.BLUE);
            g2d.fillOval(x * cellSize + cellSize / 3, y * cellSize + cellSize / 3, cellSize / 8, cellSize / 8);
            g2d.fillOval(x * cellSize + cellSize / 2 + cellSize / 8, y * cellSize + cellSize / 3, cellSize / 8, cellSize / 8);
        }
    }

    // Sets the reference to Pacman object for AI targeting calculations.
    public void setPacman(Pacman pacman) {
        this.pacmanRef = pacman;
    }
}
