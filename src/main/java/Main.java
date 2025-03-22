import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends Frame implements KeyListener {
    private BufferedImage backgroundImage;
    private Car car_me;
    private final int[] laneX = {48-20, 48+50, 48+50+48+10, 320-20, 320+48-10, 320+50+48+10};
    private int currentLane = 2;
    private int speed = 0;
    private String[] menuOptions = {"开始游戏", "退出游戏"};
    private int selectedOption = 0;
    private boolean isInMenu = true;
    private List<Car> enemyCars;
    private Random random;
    private List<String> enemyImagePaths = new ArrayList<>();
    private Timer enemySpawnTimer = new Timer();
    private volatile boolean gameRunning = false;
    private Thread gameThread;
    private Image bufferImage;
    private Graphics bufferGraphics;
    private long startTime;
    private int enemySpeed = 2;
    private int verticalSpeed = 0;
    private boolean isJumping = false;
    private int originalY;
    private Shadow shadow;
    public Main() {
        setTitle("迷你赛车");
        setSize(500, 550);
        setResizable(false);
        this.enableInputMethods(false);
        this.setFocusable(true);
        this.requestFocus();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                    gameRunning = false;
                    scheduler.shutdownNow();
                    System.exit(0);
            }
        });
        addKeyListener(this);
        try (InputStream is = Main.class.getResourceAsStream("/bg.png")) {
            if (is == null) {
                throw new IOException("资源流获取失败");
            }
            backgroundImage = ImageIO.read(is);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        currentLane = (int) (Math.random() * laneX.length);
        car_me = new Car(laneX[currentLane], 400, "role/role_me.png");
        enemyCars = new ArrayList<>();
        random = new Random();
        loadEnemyImagePaths();
        shadow = new Shadow(car_me.getX() + car_me.getWidth() / 2, car_me.getY() + car_me.getHeight() / 2, 10);
    }
    private void loadEnemyImagePaths() {
        for (int i = 1; ; i++) {
            String path = String.format("role/role_%03d.png", i);
            try (InputStream is = Main.class.getResourceAsStream("/" + path)) {
                if (is == null) break;
                enemyImagePaths.add(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private void startEnemySpawning() {
        System.out.println("敌方生成器已启动");
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            if (!isInMenu && !enemyImagePaths.isEmpty()) {
                int batchSize = 1 + random.nextInt(2);
                for (int i = 0; i < batchSize; i++) {
                    final int delayMs = 300 + random.nextInt(300);
                    scheduler.schedule(() -> {
                        EventQueue.invokeLater(() -> {
                            int lane = random.nextInt(laneX.length);
                            String imgPath = enemyImagePaths.get(
                                    random.nextInt(enemyImagePaths.size())
                            );
                            // 30%概率生成障碍物
                            if (random.nextDouble() < 0.3) {
                                imgPath = "role/role_013.png";
                            }
                            Car newEnemy = new Car(laneX[lane], -54, imgPath);
                            enemyCars.add(newEnemy);
//                            System.out.println("生成新车：" + imgPath);
                        });
                    }, delayMs, TimeUnit.MILLISECONDS);
                }
            }
        }, 0, 2, TimeUnit.SECONDS);
    }
    private void startEnemySpawning(int delay) {
        enemySpawnTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                int carsToSpawn = 2 + random.nextInt(3);
                spawnEnemyCars(carsToSpawn);
                startEnemySpawning(1000 + random.nextInt(1000));
            }
        }, delay);
    }
    private void spawnEnemyCars(int count) {
        for (int i = 0; i < count; i++) {
            enemySpawnTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    EventQueue.invokeLater(() -> {
                        int lane = random.nextInt(laneX.length);
                        String imgPath;
                        // 30%概率生成障碍物
                        if (random.nextDouble() < 0.3 &&
                                getClass().getResource("/role/role_013.png") != null) {
                            imgPath = "role/role_013.png";
                        }else {
                            imgPath = enemyImagePaths.get(random.nextInt(enemyImagePaths.size()));
                        }
                        imgPath = "role/role_013.png";
                        Car enemy = new Car(laneX[lane], -54, imgPath);
                        enemyCars.add(enemy);
                    });
                }
            }, i * 100L + random.nextInt(200));
        }
    }
    private void startGameLoop() {
        gameRunning = true;
        gameThread = new Thread(() -> {
            long lastTime = System.nanoTime();
            final double NS_PER_UPDATE = 1_000_000_000.0 / 60;
            double delta = 0;
            while (gameRunning) {
                long now = System.nanoTime();
                delta += (now - lastTime) / NS_PER_UPDATE;
                lastTime = now;
                while (delta >= 1) {
                    updateCarPosition();
                    repaint();
                    delta--;
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    System.err.println("游戏循环异常中断");
                    gameRunning = false;
                }
            }
        });
        gameThread.start();
    }
    @Override
    public void paint(Graphics g) {
        if (bufferImage == null) {
            bufferImage = createImage(getWidth(), getHeight());
            bufferGraphics = bufferImage.getGraphics();
        }
        bufferGraphics.clearRect(0, 0, getWidth(), getHeight());
        if (backgroundImage != null) {
            bufferGraphics.drawImage(backgroundImage, 0, 0, this);
        } else {
            bufferGraphics.setColor(Color.GRAY);
            bufferGraphics.fillRect(0, 0, getWidth(), getHeight());
        }
        if (isInMenu) {
            drawMenu(bufferGraphics);
        } else {
            drawGame(bufferGraphics);
        }
        g.drawImage(bufferImage, 0, 0, this);
    }
    private void drawMenu(Graphics g) {
        g.setFont(new Font("宋体", Font.BOLD, 40));
        g.setColor(Color.WHITE);
        g.drawString("迷你赛车-v1.0", 120, 80);

        g.setFont(new Font("宋体", Font.PLAIN, 20));
        g.setColor(Color.BLACK);
        g.drawString("使用 W/S(↑/↓) 选择，Enter 确认", 140, 150);

        g.setFont(new Font("宋体", Font.BOLD, 30));
        int yOffset = 200;
        for (int i = 0; i < menuOptions.length; i++) {
            g.setColor(i == selectedOption ? Color.YELLOW : Color.WHITE);
            g.drawString(menuOptions[i], 180, yOffset + i * 50);
        }
    }
    private void drawGame(Graphics g) {
        long elapsed = System.currentTimeMillis() - startTime;
        int seconds = (int)(elapsed / 1000) % 60;
        int minutes = (int)(elapsed / 1000) / 60;
        String timeStr = String.format("时间: %02d:%02d", minutes, seconds);
        g.drawString(timeStr, 15, 70);
        List<Car> normalEnemies = new ArrayList<>();
        List<Car> obstacles = new ArrayList<>();
        for (Car enemy : enemyCars) {
            if (enemy.getImagePath().contains("role_013")) {
                obstacles.add(enemy);
            } else {
                normalEnemies.add(enemy);
            }
        }
        for (Car enemy : normalEnemies) {
            enemy.draw(g);
        }
        for (Car enemy : obstacles) {
            enemy.draw(g);
        }
        car_me.draw(g);
    }
    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (isInMenu) {
            switch (key) {
                case KeyEvent.VK_W, KeyEvent.VK_UP:
                    selectedOption = (selectedOption - 1 + menuOptions.length) % menuOptions.length;
                    break;
                case KeyEvent.VK_S, KeyEvent.VK_DOWN:
                    selectedOption = (selectedOption + 1) % menuOptions.length;
                    break;
                case KeyEvent.VK_ENTER:
                    if (selectedOption == 0) {
                        isInMenu = false;
                        currentLane = (int) (Math.random() * laneX.length);
                        car_me = new Car(laneX[currentLane], 400, "role/role_me.png");
                        enemyCars.clear();
                        enemySpawnTimer.cancel();
                        enemySpawnTimer = new Timer();
                        startTime = System.currentTimeMillis();
                        startGameLoop();
                        startEnemySpawning();
                    } else if (selectedOption == 1) {
                        System.exit(0);
                    }
                    break;
            }
        } else {
            switch (key) {
                case KeyEvent.VK_A:
                    if (currentLane > 0) {
                        currentLane--;
                        car_me.setX(laneX[currentLane]);
                    }
                    break;
                case KeyEvent.VK_D:
                    if (currentLane < laneX.length - 1) {
                        currentLane++;
                        car_me.setX(laneX[currentLane]);
                    }
                    break;
                case KeyEvent.VK_W:
                    speed = -5;
                    break;
                case KeyEvent.VK_S:
                    speed = 5;
                    break;
                case KeyEvent.VK_SPACE:
                    if (!isJumping) {
                        originalY = car_me.getY();
                        verticalSpeed = -15;
                        isJumping = true;
                        car_me.startJump();
                        System.out.println("开始跳跃");
                    }
                    break;
            }
        }
        repaint();
    }
    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W || key == KeyEvent.VK_S) {
            speed = 0;
        }
        repaint();
    }
    public void updateCarPosition() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - startTime;
        enemySpeed = 2 + (int)(elapsed / 30000);
        enemySpeed = Math.min(enemySpeed, 8);
        if (!isJumping) {
            int newY = car_me.getY() + speed;
            newY = Math.max(0, Math.min(newY, getHeight() - 100));
            car_me.setY(newY);
        }
        if (isJumping) {
            int newY = car_me.getY() + verticalSpeed;
            if (newY >= originalY) {
                newY = originalY + 3;
                isJumping = false;
                verticalSpeed = 0;
                car_me.endJump();
            }
            car_me.setY(newY);
            verticalSpeed += 1;
        }
        car_me.updateShadow(originalY);
        Iterator<Car> iterator = enemyCars.iterator();
        while (iterator.hasNext()) {
            Car enemy = iterator.next();
            enemy.setY(enemy.getY() + enemySpeed);
            if (enemy.getY() > getHeight()) {
                iterator.remove();
            }
        }
    }
    @Override
    public void update(Graphics g) {
        paint(g);
    }
    public static void main(String[] args) {
        Main main = new Main();
        main.setVisible(true);
        main.setLocationRelativeTo(null);
    }
}