import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

class Car {
    private int x;
    private int y;
    private final int width = 48;
    private final int height = 54;
    private String imagePath;
    private BufferedImage image;
    private Shadow shadow;
    public Car(int x, int y, String imagePath) {
        this.x = x;
        this.y = y;
        this.imagePath = imagePath;
        this.shadow = new Shadow(x, 430, 27);
        try {
            this.image = ImageIO.read(Objects.requireNonNull(getClass().getResource("/" + imagePath)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private BufferedImage loadImage(String imagePath) {
        try (InputStream is = Car.class.getResourceAsStream(imagePath)) {
            if (is == null) {
                throw new RuntimeException("汽车图片加载失败: " + imagePath);
            }
            return ImageIO.read(is);
        } catch (IOException ex) {
            throw new RuntimeException("图片读取异常: " + imagePath, ex);
        }
    }

    public void draw(Graphics g) {
        shadow.draw(g);
        if (image != null) {
            g.drawImage(image, x, y, width, height, null);
        } else {
            g.setColor(Color.RED);
            g.fillRect(x, y, width, height);
        }
    }
    public void startJump() {
        shadow.setVisible(true);
    }
    public void endJump() {
        shadow.setVisible(false);
    }
    public void updateShadow(int originalY) {
        shadow.update(x, y, originalY, this.height);
    }
    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }

    public int getHeight() {
        return height;
    }
    public int getWidth() {
        return width;
    }

    public String getImagePath() {
        return this.imagePath;
    }
}