package Client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket socket = null;
        String host = "127.0.0.1";
        socket = new Socket(host, 4444);

        File file = new File("./src/main/resources/test.txt");
        // Get the size of the file
        long length = file.length();
        byte[] bytes = new byte[16 * 1024];
        InputStream in = new FileInputStream(file);
        OutputStream out = socket.getOutputStream();

        int count;
        byte[] rawFilename = "test.txt".getBytes(StandardCharsets.UTF_8);

        new DataOutputStream(out).writeInt(rawFilename.length);
        out.flush();
        out.write(rawFilename);
        out.flush();
        new DataOutputStream(out).writeLong(file.length());
        out.flush();
        while ((count = in.read(bytes)) > 0) {
            out.write(bytes, 0, count);
            out.flush();
        }

        out.close();
        in.close();
        socket.close();
    }
}
