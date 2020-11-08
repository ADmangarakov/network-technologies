package node;

import node.messages.user.UserMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleReceiver implements Runnable {
    private final String nodeName;
    private final UserMessageAnalyzer userMessageAnalyzer;

    public ConsoleReceiver(String nodeName, UserMessageAnalyzer userMessageAnalyzer) {
        this.nodeName = nodeName;
        this.userMessageAnalyzer = userMessageAnalyzer;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                UserMessage userMessage = new UserMessage(reader.readLine(), nodeName);
                userMessageAnalyzer.processMessageFromConsole(userMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
