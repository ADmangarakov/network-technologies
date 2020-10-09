package Server;

import Server.Exceptions.BadReceiveException;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FileReceiver implements Runnable {
    private final Socket clientSocket;
    private final InputStream data;
    private final DataInputStream dataInputStream;
    private final OutputStream answer;
    private final DataOutputStream answerDataOutputStream;
    private final InetSocketAddress inetSocketAddress;

    private static final int MSG_LEN = 4096;
    private static final int PERIOD = 3000;
    private static final String FAIL_ANSW = "FAIL";
    private static final String SUCCESS_ANSW = "SUCCESS";

    private AtomicInteger receivedBytesPerPeriod = new AtomicInteger(0);
    private AtomicLong totalBytes = new AtomicLong(0);


    public FileReceiver(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        data = clientSocket.getInputStream();
        dataInputStream = new DataInputStream(data);
        answer = clientSocket.getOutputStream();
        answerDataOutputStream = new DataOutputStream(answer);
        inetSocketAddress = new InetSocketAddress(clientSocket.getInetAddress(), clientSocket.getPort());
    }

    /*
     * Receive int as length of file name
     * Receive file length
     * Receive length byte as file
     * Write this bytes at file
     * */
    @Override
    public void run() {
        int filenameLen;
        try {
            filenameLen = dataInputStream.readInt();
        } catch (IOException e) {
            View.setStatuses(inetSocketAddress, e);
            sendAnswer(FAIL_ANSW);
            closeResources();
            return;
        }
        byte[] rawFilename = new byte[filenameLen];
        long size;
        try {
            if (data.read(rawFilename) < filenameLen) {
                View.setStatuses(inetSocketAddress,
                        new BadReceiveException("Bad file name received!"));
                sendAnswer(FAIL_ANSW);
                closeResources();
                return;
            }
            size = dataInputStream.readLong();
        } catch (IOException e) {
            View.setStatuses(inetSocketAddress, e);
            sendAnswer(FAIL_ANSW);
            closeResources();
            return;
        }
        String filename = new String(rawFilename, StandardCharsets.UTF_8);
        try {
            if (Files.exists(Paths.get("./uploads" + filename))) {
                filename += "(" + inetSocketAddress + ")";
            }
        } catch (InvalidPathException e) {
            View.setStatuses(inetSocketAddress, e);
            sendAnswer(FAIL_ANSW);
            closeResources();
        }
        File destination = new File("./uploads", filename);
        Timer timer = new Timer();
        try (FileOutputStream fileOutputStream = new FileOutputStream(destination)) {
            byte[] buff = new byte[32 * MSG_LEN];
            SpeedCounter speedCounter = new SpeedCounter();
            timer.schedule(speedCounter, PERIOD, PERIOD);
            while (totalBytes.get() < size) {
                int receivedBytes = data.read(buff, 0, MSG_LEN);
                receivedBytesPerPeriod.addAndGet(receivedBytes);
                fileOutputStream.write(buff, 0, receivedBytes);
                totalBytes.addAndGet(receivedBytes);
            }
            speedCounter.setSpeedStatistic();
            View.setFinishedStatus(inetSocketAddress);
            sendAnswer(SUCCESS_ANSW);
        } catch (IOException e) {
            View.setStatuses(inetSocketAddress, e);
            sendAnswer(FAIL_ANSW);
        } finally {
            timer.cancel();
            closeResources();
        }
    }

    private void sendAnswer(String msg) {
        try {
            answerDataOutputStream.writeInt(msg.length());
            answerDataOutputStream.flush();
            answer.write(msg.getBytes(StandardCharsets.UTF_8));
            answer.flush();
        } catch (IOException e) {
            View.setStatuses(inetSocketAddress, e);
        }
    }

    private void closeResources() {
        try {
            data.close();
            answer.close();
            clientSocket.close();
        } catch (IOException e) {
            View.setStatuses(inetSocketAddress, e);
        }
    }

    private class SpeedCounter extends TimerTask {
        private long totalTime = 0;
        private long prevTime = System.currentTimeMillis();

        @Override
        public void run() {
            setSpeedStatistic();
        }

        public synchronized void setSpeedStatistic() {
            long time = System.currentTimeMillis() - prevTime;
            BigDecimal currentSpeed = new BigDecimal(receivedBytesPerPeriod.getAndSet(0)).divide(new BigDecimal(time == 0 ? 1 : time), BigDecimal.ROUND_HALF_UP);
            totalTime += time;
            BigDecimal avgSpeed = new BigDecimal(totalBytes.get()).divide(new BigDecimal(totalTime == 0 ? 1 : totalTime), BigDecimal.ROUND_HALF_UP);
            View.setSpeedStatistic(inetSocketAddress, currentSpeed, avgSpeed);
            prevTime = System.currentTimeMillis();
        }
    }
}
