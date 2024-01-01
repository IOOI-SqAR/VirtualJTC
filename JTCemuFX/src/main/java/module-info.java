module org.jens_mueller.jtcemu.platform.fx {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.jens_mueller.JTCEMUCommons;

    opens org.jens_mueller.jtcemu.platform.fx to javafx.fxml;
    exports org.jens_mueller.jtcemu.platform.fx;
}