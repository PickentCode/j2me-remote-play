import java.io.*;
import javax.microedition.io.*;
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

public class PC_GAMING_MIDLET extends MIDlet {
    private Display display;
    private FeedCanvas canvas;
    private long lastT = 0;
    private boolean started = false;
    
    public void startApp() {
        if (started) {
            display.setCurrent(canvas);
            return;
        }
        started = true;
        
        display = Display.getDisplay(this);
        canvas = new FeedCanvas();
        display.setCurrent(canvas);
        
        new Thread(new Runnable() {
            public void run() {
                networkLoop();
            }
        }).start();
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }
    
    private void networkLoop() {
        try {
            DatagramConnection dc = (DatagramConnection) Connector.open("datagram://192.168.3.110:5555");
            int maxLen = dc.getMaximumLength();

            while (true) {
                byte[] frameBuffer = null;
                int totalChunks = -1;
                int finalLength = -1;
                final int CHUNK_SIZE = 1400;

                int chunkIndex = 0;
                while (totalChunks == -1 || chunkIndex < totalChunks) {
                    String req = "get_chunk_" + chunkIndex;
                    byte[] reqBytes = req.getBytes();
                    dc.send(dc.newDatagram(reqBytes, reqBytes.length));
                    byte[] keyByte = canvas.getKeypadStatus();
                    dc.send(dc.newDatagram(keyByte, keyByte.length));

                    Datagram dg = dc.newDatagram(maxLen);
                    dc.receive(dg);

                    byte[] data = dg.getData();
                    int total = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                    int index = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
                    int payloadLen = dg.getLength() - 4;

                    if (totalChunks == -1) {
                        totalChunks = total;
                        frameBuffer = new byte[totalChunks * CHUNK_SIZE];
                    }

                    if (index >= totalChunks) {
                        continue;
                    }

                    System.arraycopy(data, 4, frameBuffer, index * CHUNK_SIZE, payloadLen);

                    if (index == totalChunks - 1) {
                        finalLength = index * CHUNK_SIZE + payloadLen;
                    }

                    chunkIndex++;
                }

                try {
                    Image frame = Image.createImage(frameBuffer, 0, finalLength);
                    String frameInfo = "l=" + finalLength + " t=" + (System.currentTimeMillis() - lastT);
                    lastT = System.currentTimeMillis();
                    canvas.setFrame(frame, frameInfo);
                } catch (IllegalArgumentException e) { }
            }
        } catch (IOException e) {
            notifyDestroyed();
        }
    }
}