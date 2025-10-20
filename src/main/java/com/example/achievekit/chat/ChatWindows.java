package com.example.achievekit.chat;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatWindows {

    /** যে কোনো জায়গা থেকে কল দাও: ChatWindows.openCourseChat(12, "OOP - A"); */
    public static void openCourseChat(int courseId, String courseName) {
        try {
            FXMLLoader loader = new FXMLLoader(ChatWindows.class.getResource("/com/example/achievekit/StudyChat.fxml"));
            Parent root = loader.load();

            CourseChatController ctrl = loader.getController();
            ctrl.initCourse(courseId, courseName);

            Stage st = new Stage();
            st.setTitle("Study Chat • " + courseName);
            st.setScene(new Scene(root));
            st.setOnCloseRequest(e -> ctrl.shutdown());
            st.setMaximized(false);
            st.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
