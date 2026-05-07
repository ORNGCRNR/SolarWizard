module com.solarwizard {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.solarwizard.app        to javafx.graphics, javafx.fxml;
    opens com.solarwizard.view       to javafx.graphics, javafx.fxml;
    opens com.solarwizard.view.steps to javafx.graphics, javafx.fxml;
    opens com.solarwizard.model      to javafx.graphics, javafx.fxml;

    exports com.solarwizard.app;
    exports com.solarwizard.view;
    exports com.solarwizard.view.steps;
    exports com.solarwizard.model;
    exports com.solarwizard.service;
    exports com.solarwizard.util;
}
