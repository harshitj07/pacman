// Import packages
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

// Pacman class represents the player-controlled character in a Pacman game. It Hhndles movement, animation, collision detection, and rendering.
public class Pacman {
    // Position coordinates on the game grid
    private int x, y;
    
    // Current movement direction (-1, 0, or 1 for each axis)
    private int dx, dy;
    
    // Animation variables for mouth opening/closing effect
    private int mouthAngle;
    private boolean mouthOpening;
    private static final int MOUTH_SPEED = 3;
    private static final int MAX_MOUTH_ANGLE = 40;
    
    // Direction tracking: 0=right, 1=down, 2=left, 3=up
    private int currentDirection;
    
    // Storage for sprite images organized by direction
    private Map<Integer, BufferedImage[]> directionImages;
    
    // Animation frame control
    private int animationFrame = 0;
    private static final int ANIMATION_DELAY = 2; // Frames to wait between animation updates
    private int animationCounter = 0;
    
    // Buffered input - stores the next desired direction
    private int nextDx, nextDy;
    
    // Maze dimensions constant
    private static final int MAZE_WIDTH = 28;

    // Constructor initializes Pacman at the specified grid position
    public Pacman(int x, int y) {
        this.x = x;
        this.y = y;
        this.dx = 0;  // Start stationary
        this.dy = 0;
        this.nextDx = 0;  // No queued movement initially
        this.nextDy = 0;
        this.mouthAngle = 0;
        this.mouthOpening = true;
        this.currentDirection = 0;  // Default facing right
        this.directionImages = new HashMap<>();
        loadImages();
    }

    // Loads sprite images for each direction from the file system. Each direction has 3 animation frames for the mouth opening/closing effect.
    private void loadImages() {
        try {
            // Load images for each of the four directions
            String[] directions = {"right", "down", "left", "up"};
            for (int i = 0; i < directions.length; i++) {
                BufferedImage[] frames = new BufferedImage[3];
                for (int j = 0; j < 3; j++) {
                    // Load each frame image from the corresponding directory
                    frames[j] = ImageIO.read(new File("images/pacman-" + directions[i] + "/" + (j + 1) + ".png"));
                }
                directionImages.put(i, frames);
            }
        } catch (IOException e) {
            System.err.println("Error loading Pacman images: " + e.getMessage());
        }
    }

    // Sets the desired movement direction for Pacman.
    public void setDirection(int dx, int dy) {
        // Store the desired direction for later application
        this.nextDx = dx;
        this.nextDy = dy;
        
        // Update the current direction for animation/rendering purposes
        if (dx > 0) currentDirection = 0;      // Moving right
        else if (dy > 0) currentDirection = 1; // Moving down
        else if (dx < 0) currentDirection = 2; // Moving left
        else if (dy < 0) currentDirection = 3; // Moving up
    }

    // Handles Pacman's movement logic including collision detection and tunnel teleportation.
    public void move(int[][] maze) {
        // First, try to move in the desired direction (buffered input)
        int newX = x + nextDx;
        int newY = y + nextDy;

        // Handle horizontal tunnel teleportation at row 14
        if (newX < 0 && y == 14) { // Exiting left side of maze
            newX = MAZE_WIDTH - 1;  // Teleport to right side
        } else if (newX >= MAZE_WIDTH && y == 14) { // Exiting right side of maze
            newX = 0;  // Teleport to left side
        }

        // If the desired direction is valid, move there and update current direction
        if (isValidMove(newX, newY, maze)) {
            x = newX;
            y = newY;
            dx = nextDx;  // Apply the buffered direction
            dy = nextDy;
        } else {
            // If desired direction is blocked, try to continue in current direction
            newX = x + dx;
            newY = y + dy;

            // Handle tunnel teleportation for current direction movement
            if (newX < 0 && y == 14) { // Left tunnel
                newX = MAZE_WIDTH - 1;
            } else if (newX >= MAZE_WIDTH && y == 14) { // Right tunnel
                newX = 0;
            }
            
            // Move in current direction if valid (allows smooth movement along corridors)
            if (isValidMove(newX, newY, maze)) {
                x = newX;
                y = newY;
            }
            // If neither direction is valid, Pacman stops moving
        }
        
        // Update animation frame for sprite cycling
        animationCounter++;
        if (animationCounter >= ANIMATION_DELAY) {
            animationFrame = (animationFrame + 1) % 3;  // Cycle through 3 frames
            animationCounter = 0;
        }
    }

    // Checks if a move to the specified coordinates is valid. Handles boundary checking, wall collision, and ghost safe zone restrictions.
    private boolean isValidMove(int newX, int newY, int[][] maze) {
        // Special case: allow movement into tunnel areas at row 14
        if ((newX < 0 || newX >= maze[0].length) && y == 14) {
            return true; // Tunnel movement is always valid
        }
        
        // Check basic boundary conditions
        if (newX < 0 || newX >= maze[0].length || newY < 0 || newY >= maze.length) {
            return false;
        }
        
        // Prevent Pacman from entering the ghost safe zone (center box)
        // This is typically where ghosts respawn and Pacman shouldn't enter
        boolean isMovingToSafeZone = (newY >= 12 && newY <= 16 && newX >= 11 && newX <= 17);
        if (isMovingToSafeZone) {
            return false; // Block movement into ghost safe area
        }
        
        // Check if the target cell is not a wall (1 = wall, 0 = empty space)
        return maze[newY][newX] != 1;
    }

    // Renders Pacman on the screen using either sprite images or fallback graphics.
    public void draw(Graphics2D g2d, int cellSize) {
        // Try to use loaded sprite images first
        if (directionImages.containsKey(currentDirection)) {
            BufferedImage[] frames = directionImages.get(currentDirection);
            // Draw the current animation frame at Pacman's grid position
            g2d.drawImage(frames[animationFrame], x * cellSize, y * cellSize, cellSize, cellSize, null);
        } else {
            // Fallback to drawing a simple yellow arc if images failed to load
            g2d.setColor(Color.YELLOW);
            int startAngle = currentDirection * 90;  // Rotate based on direction
            int arcAngle = 360 - mouthAngle * 2;     // Create mouth opening
            g2d.fillArc(x * cellSize, y * cellSize, cellSize, cellSize, 
                        startAngle + mouthAngle, arcAngle);
        }
    }

    // Current X coordinate on the game grid
    public int getX() {
        return x;
    }

    // Current Y coordinate on the game grid
    public int getY() {
        return y;
    }
    
    // Current horizontal movement direction (-1, 0, or 1)
    public int getDirectionX() {
        return dx;
    }
    
    // Current vertical movement direction (-1, 0, or 1)
    public int getDirectionY() {
        return dy;
    }

    // Resets Pacman to the starting position and state.
    public void reset() {
        // Reset to starting position (center-bottom of maze)
        this.x = 14; // Horizontal center of the 28-wide maze
        this.y = 23; // Near the bottom of the maze
        this.dx = 0;     // Stop all movement
        this.dy = 0;
        this.nextDx = 0; // Clear buffered input
        this.nextDy = 0;
        this.currentDirection = 0; // Face right by default
    }
}
