package com.example.achievekit.chat;

/** line protocol: type|room|sender|timestamp|body
 * room = "course:<CourseID>"
 */
public class Message {
    public enum Type { HELLO, CHAT, SYSTEM, HISTORY }

    private Type type;
    private String room;     // "course:12"
    private String sender;   // username
    private long timestamp;  // epoch millis
    private String body;

    public Message() {}
    public Message(Type t, String room, String sender, long timestamp, String body) {
        this.type=t; this.room=room; this.sender=sender; this.timestamp=timestamp; this.body=body;
    }

    public static Message hello(String room, String sender){
        return new Message(Type.HELLO, room, sender, System.currentTimeMillis(), "HELLO");
    }
    public static Message chat(String room, String sender, String body, long ts){
        return new Message(Type.CHAT, room, sender, ts, body);
    }
    public static Message system(String room, String body){
        return new Message(Type.SYSTEM, room, "system", System.currentTimeMillis(), body);
    }
    public static Message history(String room, String sender, String body, long ts){
        return new Message(Type.HISTORY, room, sender, ts, body);
    }

    public String encode(){
        return type.name()+"|"+nz(room)+"|"+nz(sender)+"|"+timestamp+"|"+esc(nz(body));
    }
    public static Message decode(String line){
        try{
            String[] p = line.split("\\|",5);
            return new Message(Type.valueOf(p[0]), p[1], p[2], Long.parseLong(p[3]), unesc(p[4]));
        }catch(Exception e){ return null; }
    }

    private static String esc(String s){ return s.replace("\\","\\\\").replace("|","\\p").replace("\n","\\n").replace("\r",""); }
    private static String unesc(String s){ return s.replace("\\n","\n").replace("\\p","|").replace("\\\\","\\"); }
    private static String nz(String s){ return s==null? "" : s; }

    public Type getType(){ return type; }
    public String getRoom(){ return room; }
    public String getSender(){ return sender; }
    public long getTimestamp(){ return timestamp; }
    public String getBody(){ return body; }
}
