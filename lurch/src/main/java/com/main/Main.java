package com.lurch;

import java.util.ArrayList;
import java.util.List;

import com.lurch.display.Quad;
import com.lurch.display.Shader;
import com.lurch.display.Window;
import com.lurch.time.Timer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class Main 
{
    private final Window window;
    private final Shader colorShader;
    private final Timer timer;
    private final Quad quad;

    private boolean running = true;
    private int[][] grid = new int[4][4];
    private boolean moved = false;
    private float moveCooldown = 0.0f;
    private final float moveDelay = 0.5f;

    // Define colors for different tile values
    private static final Vector3f[] TILE_COLORS = 
    {
        new Vector3f(0.30f, 0.30f, 0.30f), // 0 (empty)
        new Vector3f(0.93f, 0.89f, 0.85f), // 2
        new Vector3f(0.93f, 0.87f, 0.80f), // 4
        new Vector3f(0.96f, 0.78f, 0.64f), // 8
        new Vector3f(0.96f, 0.65f, 0.45f), // 16
        new Vector3f(0.96f, 0.55f, 0.33f), // 32
        new Vector3f(0.96f, 0.45f, 0.24f), // 64
        new Vector3f(0.93f, 0.80f, 0.40f), // 128
        new Vector3f(0.93f, 0.78f, 0.35f), // 256
        new Vector3f(0.93f, 0.75f, 0.30f), // 512
        new Vector3f(0.93f, 0.72f, 0.25f), // 1024
        new Vector3f(0.93f, 0.70f, 0.20f)  // 204811
    };

    public Main()
    {
        window = new Window("2048");
        colorShader = new Shader("color");
        timer = new Timer();
        quad = new Quad();

        timer.setUPS(60f);
    }

    public void run() 
    {
        init();
        loop();
        free();
    }

    private void init() 
    {
        colorShader.compile();
        timer.start();

        colorShader.bind();
        float boardSize = 440f; // 4 tiles * 100 + 4 gaps * 10
        float offsetX = (800f - boardSize) / 2;
        float offsetY = (800f - boardSize) / 2;
        Matrix4f projection = new Matrix4f().ortho2D(-window.getWidth()/2f, window.getWidth()/2f, -window.getHeight()/2f, window.getHeight()/2f);
        colorShader.setUniformMatrix4f("u_projection", projection);
        colorShader.unbind();

        spawnRandomTile();
        spawnRandomTile();
    }

    private void loop() 
    {
        while (!window.shouldClose()) {
            window.clear();

            handleInput();

            timer.update();
            if (running) {
                int updates = timer.getAccumulatedUpdates();
                for (int i = 0; i < updates; i++) 
                {
                    if (!running) break;
                    update();
                }
            }

            timer.consume();
            render();
            window.refresh();
        }
    }

    private void update() 
    {
        // Decrease move cooldown
        if (moveCooldown > 0) {
            moveCooldown -= 1.0f / timer.getUPS();
            if (moveCooldown < 0) moveCooldown = 0;
        }
    }

    private void render()
    {
        colorShader.bind();
        
        // Draw background grid
        float boardSize = 440f;
        float offsetX = -boardSize/2;
        float offsetY = -boardSize/2;
        
        // Draw background
        colorShader.setUniform3f("u_color", 0.15f, 0.15f, 0.15f);
        colorShader.setUniformMatrix4f("u_model", 
            new Matrix4f().translate(offsetX, offsetY, 0.0f).scale(boardSize, boardSize, 1.0f));
        quad.render();

        // Draw empty tiles
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                int value = grid[y][x];
                float px = offsetX + x * 110 + 10;
                float py = offsetY + y * 110 + 10;
                
                // Get color based on tile value
                int colorIndex = value == 0 ? 0 : (int)(Math.log(value) / Math.log(2));
                colorIndex = Math.min(colorIndex, TILE_COLORS.length - 1);
                Vector3f color = TILE_COLORS[colorIndex];
                
                colorShader.setUniform3f("u_color", color.x, color.y, color.z);
                colorShader.setUniformMatrix4f("u_model", 
                    new Matrix4f().translate(px, py, 0.0f).scale(90f, 90f, 1.0f));
                quad.render();
            }
        }

        colorShader.unbind();
    }

    private void spawnRandomTile() {
        List<int[]> empty = new ArrayList<>();
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                if (grid[y][x] == 0) empty.add(new int[] {x, y});
            }
        }
        if (empty.isEmpty()) {
            running = false;
            return;
        }

        int[] pos = empty.get((int)(Math.random() * empty.size()));
        grid[pos[1]][pos[0]] = Math.random() < 0.9 ? 2 : 4;
    }

    private void handleInput()
    {
        // Only process input if move cooldown has expired
        if (moveCooldown > 0) return;

        moved = false;
        if (window.isKeyPressed(GLFW.GLFW_KEY_UP)) moveUp();
        else if (window.isKeyPressed(GLFW.GLFW_KEY_DOWN)) moveDown();
        else if (window.isKeyPressed(GLFW.GLFW_KEY_LEFT)) moveLeft();
        else if (window.isKeyPressed(GLFW.GLFW_KEY_RIGHT)) moveRight();

        if (moved) 
        {
            spawnRandomTile();
            moveCooldown = moveDelay; // Set cooldown after a move
        }
    }

    private void moveLeft() 
    {
        for (int y = 0; y < 4; y++) {
            int[] row = grid[y];
            int[] newRow = new int[4];
            int pos = 0;
            boolean merged = false;

            for (int x = 0; x < 4; x++) {
                if (row[x] == 0) continue;
                if (pos > 0 && newRow[pos - 1] == row[x] && !merged) {
                    newRow[pos - 1] *= 2;
                    merged = true;
                    moved = true;
                } else {
                    newRow[pos++] = row[x];
                    if (pos - 1 != x) moved = true;
                    merged = false;
                }
            }
            grid[y] = newRow;
        }
    }

    private void moveRight() 
    {
        for (int y = 0; y < 4; y++) {
            int[] row = grid[y];
            int[] newRow = new int[4];
            int pos = 3;
            boolean merged = false;

            for (int x = 3; x >= 0; x--) 
            {
                if (row[x] == 0) continue;

                if (pos < 3 && newRow[pos + 1] == row[x] && !merged) 
                {
                    newRow[pos + 1] *= 2;
                    merged = true;
                    moved = true;
                } else 
                {
                    newRow[pos--] = row[x];
                    if (pos + 1 != x) moved = true;
                    merged = false;
                }
            }
            grid[y] = newRow;
        }
    }

    private void moveDown() 
    {
        for (int x = 0; x < 4; x++) {
            int[] col = new int[4];
            for (int y = 0; y < 4; y++) col[y] = grid[y][x];
            int[] newCol = new int[4];
            int pos = 0;
            boolean merged = false;

            for (int y = 0; y < 4; y++) 
            {
                if (col[y] == 0) continue;

                if (pos > 0 && newCol[pos - 1] == col[y] && !merged) 
                {
                    newCol[pos - 1] *= 2;
                    merged = true;
                    moved = true;
                } else 
                {
                    newCol[pos++] = col[y];
                    if (pos - 1 != y) moved = true;
                    merged = false;
                }
            }
            for (int y = 0; y < 4; y++) grid[y][x] = newCol[y];
        }
    }

    private void moveUp() 
    {
        for (int x = 0; x < 4; x++) 
        {
            int[] col = new int[4];
            for (int y = 0; y < 4; y++) col[y] = grid[y][x];
            int[] newCol = new int[4];
            int pos = 3;
            boolean merged = false;

            for (int y = 3; y >= 0; y--) 
            {
                if (col[y] == 0) continue;

                if (pos < 3 && newCol[pos + 1] == col[y] && !merged) 
                {
                    newCol[pos + 1] *= 2;
                    merged = true;
                    moved = true;
                } else 
                {
                    newCol[pos--] = col[y];
                    if (pos + 1 != y) moved = true;
                    merged = false;
                }
            }
            for (int y = 0; y < 4; y++) grid[y][x] = newCol[y];
        }
    }

    private void free() 
    {
        window.delete();
        colorShader.delete();
        quad.delete();
    }

    public static void main(String[] args) 
    {
        new Main().run();
    }
}
