package com.example.achievekit.chat;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ChatClient {
    private final String host;
    private final int port;
    private final String room;      // "course:<CourseID>"
    private final String username;
    private final Consumer<Message> onMessage;

    private volatile boolean connected=false;
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private final ExecutorService ioPool = Executors.newSingleThreadExecutor();

    public ChatClient(String host, int port, String room, String username, Consumer<Message> onMessage) {
        this.host=host; this.port=port; this.room=room; this.username=username; this.onMessage=onMessage;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        // hello
        writeLine(Message.hello(room, username).encode());
        connected=true;

        ioPool.submit(() -> {
            try {
                String line;
                while (connected && (line=in.readLine())!=null) {
                    Message m = Message.decode(line);
                    if (m!=null && onMessage!=null) onMessage.accept(m);
                }
            } catch (IOException ignored) {
            } finally {
                connected=false;
                try { if (socket!=null) socket.close(); } catch (IOException ignored2) {}
            }
        });
    }

    public void send(Message m){
        if(!connected) return;
        try { writeLine(m.encode()); } catch (IOException ignored) {}
    }

    private synchronized void writeLine(String s) throws IOException {
        out.write(s); out.write('\n'); out.flush();
    }

    public void close() {
        connected=false;
        try { if (socket!=null) socket.close(); } catch (IOException ignored) {}
        ioPool.shutdownNow();
    }

    public boolean isConnected(){ return connected; }
    public String getRoom(){ return room; }
}
