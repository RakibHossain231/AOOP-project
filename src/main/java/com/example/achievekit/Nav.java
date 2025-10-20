package com.example.achievekit;

import javafx.scene.control.Button;

public final class Nav {
    private Nav() {}

    public static void highlight(Button active, Button... all) {
        for (Button b : all) {
            if (b == null) continue;
            b.getStyleClass().remove("nav-item-active");
        }
        if (active != null && !active.getStyleClass().contains("nav-item-active")) {
            active.getStyleClass().add("nav-item-active");
        }
    }
}
