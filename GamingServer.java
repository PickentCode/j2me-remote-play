package Server;

import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class GamingServer {
	static final int CHUNK_SIZE = 1400; // 1452 max (elm)
	static final int KEYPAD_OFFSET = 10;
	static byte[] keypadHistory = new byte[64 + KEYPAD_OFFSET];
	static Robot robot;
	static int mouseVelX = 0, mouseVelY = 0, mouseSpeed = 2;

	public static void main(String args[]) throws IOException {
		System.out.println("Starting the server...");

		DatagramSocket ds = new DatagramSocket(5555);
		byte[] receive = new byte[65535];

		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}

		byte[] jpegBytes = null;
		int totalChunks = 0;

		DatagramPacket dpReceive = null;
		while (true) {
			dpReceive = new DatagramPacket(receive, receive.length);
			ds.receive(dpReceive);
			InetAddress clientAddress = dpReceive.getAddress();
			int clientPort = dpReceive.getPort();
			String msg = data(receive).toString();

			if (msg.equals("close")) {
				System.out.println("Closing the server...");
				break;
			} else if (msg.startsWith("get_chunk_")) {
				int i = Integer.parseInt(msg.substring("get_chunk_".length()));

				if (i == 0) {
					BufferedImage frame = getFrame();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();

					ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
					ImageWriteParam param = writer.getDefaultWriteParam();
					if (param.canWriteCompressed()) {
					    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					    param.setCompressionQuality(0.1f);
					}
					try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
					    writer.setOutput(ios);
					    writer.write(null, new IIOImage(frame, null, null), param);
					} finally {
					    writer.dispose();
					}
					jpegBytes = baos.toByteArray();
					
					//ImageIO.write(frame, "jpg", baos);
					//jpegBytes = baos.toByteArray();
					totalChunks = (int) Math.ceil(jpegBytes.length / (double) CHUNK_SIZE);
				}

				if (jpegBytes == null || i >= totalChunks) {
					System.out.println("bad request");
				} else {
					System.out.println("sending chunk " + i + "/" + totalChunks);
					sendChunk(ds, jpegBytes, totalChunks, i, clientAddress, clientPort);
				}
			} else {
				simulateKeyboard(receive);
				updateMousePosition();
			}

			receive = new byte[65535];
		}
		ds.close();
	}
	
	private static void simulateKeyboard(byte[] keypadStatus) {
	    try {
	        for (int i = 0; i < 64 + KEYPAD_OFFSET; i++) {
	            if (keypadStatus[i] == keypadHistory[i]) continue;
	            keypadHistory[i] = keypadStatus[i];

	            boolean pressed = (keypadStatus[i] == 1);
	            switch (i) {
	            	case 2 -> { if (pressed) robot.keyPress(KeyEvent.VK_ESCAPE); else robot.keyRelease(KeyEvent.VK_ESCAPE); }
	                case 3 -> { if (pressed) robot.mousePress(InputEvent.BUTTON3_DOWN_MASK); else robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK); }
	                case 4 -> { if (pressed) robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); else robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK); }
	                case 5 -> { if (pressed) robot.keyPress(KeyEvent.VK_ENTER); else robot.keyRelease(KeyEvent.VK_ENTER); }
//	                case 9 -> { if (pressed) robot.keyPress(KeyEvent.VK_UP); else robot.keyRelease(KeyEvent.VK_UP); }
//	                case 8 -> { if (pressed) robot.keyPress(KeyEvent.VK_DOWN); else robot.keyRelease(KeyEvent.VK_DOWN); }
//	                case 7 -> { if (pressed) robot.keyPress(KeyEvent.VK_LEFT); else robot.keyRelease(KeyEvent.VK_LEFT); }
//	                case 6 -> { if (pressed) robot.keyPress(KeyEvent.VK_RIGHT); else robot.keyRelease(KeyEvent.VK_RIGHT); }
	                case 9 -> mouseVelY = pressed ? -mouseSpeed : 0;
	                case 8 -> mouseVelY = pressed ? mouseSpeed : 0;
	                case 7 -> mouseVelX = pressed ? -mouseSpeed : 0;
	                case 6 -> mouseVelX = pressed ? mouseSpeed : 0;
	                case 59 -> { if (pressed) robot.keyPress(KeyEvent.VK_TAB); else robot.keyRelease(KeyEvent.VK_TAB); }
	                case 60 -> { if (pressed) robot.keyPress(KeyEvent.VK_W); else robot.keyRelease(KeyEvent.VK_W); }
	                case 61 -> { if (pressed) robot.keyPress(KeyEvent.VK_F); else robot.keyRelease(KeyEvent.VK_F); }
	                case 62 -> { if (pressed) robot.keyPress(KeyEvent.VK_A); else robot.keyRelease(KeyEvent.VK_A); }
	                case 63 -> { if (pressed) robot.keyPress(KeyEvent.VK_SPACE); else robot.keyRelease(KeyEvent.VK_SPACE); }
	                case 64 -> { if (pressed) robot.keyPress(KeyEvent.VK_D); else robot.keyRelease(KeyEvent.VK_D); }
	                case 66 -> { if (pressed) robot.keyPress(KeyEvent.VK_S); else robot.keyRelease(KeyEvent.VK_S); }
	            }
	        }
	    } catch (Exception ignored) {}
	}
	
	public static void updateMousePosition() {
	    if (mouseVelX != 0 || mouseVelY != 0) {
	        Point p = MouseInfo.getPointerInfo().getLocation();
	        //robot.mouseMove(p.x + mouseVelX, p.y + mouseVelY); // for desktop
	        robot.mouseMove(mouseVelX, mouseVelY); // for gta
	    }
	}

	private static void sendChunk(DatagramSocket ds, byte[] jpegBytes, int totalChunks, int i,
			InetAddress clientAddress, int clientPort) throws IOException {
		int start = i * CHUNK_SIZE;
		int end = Math.min(start + CHUNK_SIZE, jpegBytes.length);
		int payloadLen = end - start;

		byte[] chunk = new byte[payloadLen + 4];
		chunk[0] = (byte) (totalChunks >> 8);
		chunk[1] = (byte) (totalChunks & 0xFF);
		chunk[2] = (byte) (i >> 8);
		chunk[3] = (byte) (i & 0xFF);
		System.arraycopy(jpegBytes, start, chunk, 4, payloadLen);

		ds.send(new DatagramPacket(chunk, chunk.length, clientAddress, clientPort));
	}
	
	public static BufferedImage getFrame() {
		//Rectangle capture = new Rectangle(0, 0, 640, 480);
		Rectangle capture = new Rectangle(2560/2 - 800 / 2, 1440/2 - 600 / 2, 800, 600);
		BufferedImage frame = robot.createScreenCapture(capture);
		int fw = (int)(240);
		int fh = (int)((double)fw * (240.0 / 320.0));
		//fw = (int)(320.0 * 1.6);
		//fh = (int)(240.0 * 1.6);
		return scale(frame, fw, fh);
	}
	
	public static BufferedImage scale(BufferedImage src, int w, int h)
	{
	    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
	    int x, y;
	    int ww = src.getWidth();
	    int hh = src.getHeight();
	    int[] ys = new int[h];
	    for (y = 0; y < h; y++)
	        ys[y] = y * hh / h;
	    for (x = 0; x < w; x++) {
	        int newX = x * ww / w;
	        for (y = 0; y < h; y++) {
	            int col = src.getRGB(newX, ys[y]);
	            img.setRGB(x, y, col);
	        }
	    }
	    return img;
	}

	public static StringBuilder data(byte[] a) {
		if (a == null) return null;
		StringBuilder result = new StringBuilder();
		int i = 0;
		while (a[i] != 0) {
			result.append((char) a[i]);
			i++;
		}
		return result;
	}
}