module com.example.achievekit {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;   // safe: some setups need this explicitly
    requires javafx.base;       // safe: models/props use this
    requires java.sql;
    requires jbcrypt;
    requires java.prefs;
    requires java.desktop;
    requires javafx.media;

    // FXML controllers যেসব প্যাকেজে আছে, সেগুলো 'opens' করো
    opens com.example.achievekit to javafx.fxml;
    opens com.example.achievekit.chat to javafx.fxml;

    // public API/exported packages
    exports com.example.achievekit;
    exports com.example.achievekit.chat;
    exports com.example.achievekit.util;
    opens com.example.achievekit.util to javafx.fxml;
}
