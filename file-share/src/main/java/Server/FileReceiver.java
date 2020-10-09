package Server;

import Server.Exceptions.BadReceiveException;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Time;
import java.util.Date;

public class FileReceiver implements Runnable {
    private final Socket clientSocket;
    private final InputStream data;
    private final OutputStream answer;
    private final InetSocketAddress inetSocketAddress;

    private static final int MSG_LEN = 4096;

    public FileReceiver(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        data = clientSocket.getInputStream();
        answer = clientSocket.getOutputStream();
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
        DataInputStream dataInputStream = new DataInputStream(data);
        int filenameLen;
        try {
            filenameLen = dataInputStream.readInt();
        } catch (IOException e) {
            View.setStatuses(inetSocketAddress, e);
            return;
        }
        byte[] rawFilename = new byte[filenameLen];
        long size = 0;
        try {
            if (data.read(rawFilename) < filenameLen) {
                View.setStatuses(inetSocketAddress,
                        new BadReceiveException("Bad file name received!"));
            }
            size = dataInputStream.readLong();
        } catch (IOException e) {
            View.setStatuses(inetSocketAddress, e);
            closeResources();
            return;
        }
        String filename = new String(rawFilename, StandardCharsets.UTF_8);
        if (Files.exists(Path.of("./src/main/resources/uploads/" + filename))) {
            filename += "(" + inetSocketAddress + ")";
        }
        File destination = new File("./src/main/resources/uploads", filename);
        try (FileOutputStream fileOutputStream = new FileOutputStream(destination)) {
            long totalBytes = 0;
            byte[] buff = new byte[MSG_LEN];
            Date startTime = new Date(System.currentTimeMillis());
            while (totalBytes < size) {
                int receivedBytes = data.read(buff, 0, MSG_LEN);
                fileOutputStream.write(buff, 0, receivedBytes);
                totalBytes += receivedBytes;
            }
        } catch (IOException e) {
            e.printStackTrace();
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
}
