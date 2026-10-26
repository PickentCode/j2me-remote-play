import javax.microedition.lcdui.*;

public class FeedCanvas extends Canvas {
    private Image frame;
    private String frameInfo;
    final private int keypadOffset = 10;
    private byte[] keypadStatus = new byte[64 + keypadOffset];
    
    public void setFrame(Image img, String data) {
        frame = img;
        frameInfo = data;
        repaint();
    }
    
    protected void paint(Graphics g) {
        g.setColor(0, 0, 0);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (frame != null) {
            g.drawImage(frame, 0, 0, Graphics.TOP | Graphics.LEFT);
            g.setColor(0x0099ff);
            g.drawString(frameInfo, 10, 195, Graphics.TOP | Graphics.LEFT);
            g.drawString(("Key Val: " + keypadStatusString()), 10, 230, Graphics.TOP | Graphics.LEFT);
            g.fillRect(0, 0, getWidth(), 5);
            g.fillRect(0, getHeight() - 5, getWidth(), 5);
            g.fillRect(0, 178, getWidth(), 5);
            g.fillRect(0, 0, 5, getHeight());
            g.fillRect(getWidth() - 5, 0, 5, getHeight());
        }
    }
    
    protected void keyPressed(int keyCode) {
        int key = keyCode + keypadOffset;
        if (key >= 0 && key < keypadStatus.length) {
            keypadStatus[key] = 1;
        }
    }
    
    protected void keyReleased(int keyCode) {
        int key = keyCode + keypadOffset;
        if (key >= 0 && key < keypadStatus.length) {
            keypadStatus[key] = 0;
        }
    }
    
    public String keypadStatusString() {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < keypadStatus.length; i++) {
            if (keypadStatus[i] == 1) result.append(i).append(", ");
        }
        return result.toString();
    }
    
    public byte[] getKeypadStatus() {
        return keypadStatus;
    }
}