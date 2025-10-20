package com.example.achievekit.chatserver;

import com.example.achievekit.chat.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class ChatServer {

    private final int port;
    private volatile boolean running = true;

    // roomKey -> connected clients list
    private final ConcurrentMap<String, CopyOnWriteArrayList<ClientHandler>> rooms = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newCachedThreadPool();

    private final CourseChatRepository repo = new CourseChatRepository();

    public ChatServer(int port) { this.port = port; }

    public void start() {
        System.out.println("[ChatServer] Listening on " + port);
        try (ServerSocket server = new ServerSocket(port)) {
            while (running) {
                Socket socket = server.accept();
                pool.submit(new ClientHandler(socket));
            }
        } catch (IOException e) {
            System.err.println("[ChatServer] Error: " + e.getMessage());
        } finally {
            pool.shutdownNow();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private BufferedWriter out;

        private String roomKey;  // "course:<id>"
        private int courseId;
        private String username;
        private Integer userID; // users.userID

        ClientHandler(Socket socket) { this.socket = socket; }

        @Override public void run() {
            try {
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

                // Expect HELLO
                Message hello = Message.decode(in.readLine());
                if (hello==null || hello.getType()!= Message.Type.HELLO) { socket.close(); return; }

                this.roomKey = hello.getRoom(); // "course:12"
                this.username = hello.getSender();
                this.courseId = parseCourseId(roomKey);
                if (courseId <= 0) { socket.close(); return; }

                this.userID = repo.findUserIDByUsername(username);
                if (userID == null) { socket.close(); return; }

                // ensure room structure
                repo.ensureMember(courseId, userID);

                rooms.putIfAbsent(roomKey, new CopyOnWriteArrayList<>());
                rooms.get(roomKey).add(this);

                // 1) send history to this client only
                for (Message h : repo.recentMessages(courseId, 100)) write(h);

                // 2) broadcast join info
                broadcast(roomKey, Message.system(roomKey, username + " joined. Members: " + rooms.get(roomKey).size()));

                // loop
                String line;
                while ((line = in.readLine()) != null) {
                    Message m = Message.decode(line);
                    if (m == null) continue;
                    if (m.getType() == Message.Type.CHAT) {
                        // save to DB
                        try { repo.saveMessage(courseId, userID, m.getBody(), m.getTimestamp()); } catch (Exception ignored) {}
                        // send to all (client ignores own echo)
                        broadcast(roomKey, m);
                    }
                }
            } catch (IOException ignored) {
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        private int parseCourseId(String key) {
            try {
                if (key!=null && key.startsWith("course:")) return Integer.parseInt(key.substring("course:".length()));
            } catch (Exception ignored) {}
            return -1;
        }

        private void write(Message m) {
            try { out.write(m.encode()); out.write('\n'); out.flush(); } catch (IOException ignored) {}
        }

        private void disconnect() {
            try { socket.close(); } catch (IOException ignored) {}
            var list = rooms.get(roomKey);
            if (list != null) {
                list.remove(this);
                broadcast(roomKey, Message.system(roomKey, username + " left. Members: " + list.size()));
            }
        }
    }

    private void broadcast(String roomKey, Message m) {
        var list = rooms.get(roomKey);
        if (list == null) return;
        for (ClientHandler ch : list) ch.write(m);
    }
}
