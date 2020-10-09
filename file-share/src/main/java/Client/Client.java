package Client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("You have to write <path>, <filename>, <IP> and <port>");
            return;
        }
        Socket socket;
        String host = args[2];
        socket = new Socket(host, Integer.parseInt(args[3]));

        File file = new File(args[0]);
        // Get the size of the file
        long length = file.length();
        byte[] bytes = new byte[16 * 1024];
        InputStream in = new FileInputStream(file);
        OutputStream out = socket.getOutputStream();

        int count;
        byte[] rawFilename = args[1].getBytes(StandardCharsets.UTF_8);

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
        int len = new DataInputStream(socket.getInputStream()).readInt();
        byte[] answer = new byte[len];
        socket.getInputStream().read(answer, 0 ,len);
        System.out.println("Server answer: " + new String(answer, StandardCharsets.UTF_8));

        out.close();
        in.close();
        socket.close();
    }
}
