import java.awt.*;

public class Shadow {
    private int x;
    private int y;
    private int baseSize;
    private float alpha;
    private Color color;
    private boolean visible = false;
    private int shadowWidth;
    private int shadowHeight;
    public Shadow(int baseX, int baseY, int baseSize) {
        this.x = baseX;
        this.y = baseY;
        this.baseSize = baseSize;
        this.color = new Color(0, 0, 0, 0.3f);
    }
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    public void update(int carX, int carY, int originalY, int carHeight) {
        this.x = carX + 15;
        float jumpHeight = (originalY - carY) / 100f;
        jumpHeight = Math.max(0, Math.min(jumpHeight, 1));

        this.alpha = 0.8f - jumpHeight * 0.3f;
        float lengthScale = 1.5f + jumpHeight * 2.5f;
        this.shadowWidth = (int)(baseSize * (1.2f - jumpHeight * 0.3f));
        this.shadowHeight = (int)(baseSize * lengthScale);
        this.y = originalY + carHeight - 5;
    }
    public void draw(Graphics g) {
        if (!visible) return;
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        GradientPaint gradient = new GradientPaint(
                x, y, new Color(0, 0, 0, alpha * 0.8f),
                x, y - shadowHeight, new Color(0, 0, 0, 0)
        );
        g2d.setPaint(gradient);
        int drawY = y - shadowHeight;
        g2d.fillOval(x - shadowWidth/2, drawY, shadowWidth, shadowHeight);
        g2d.dispose();
    }
}