module jtcemu {
    requires java.logging;
    requires java.naming;
    requires java.desktop;
    requires javafx.web;

    exports org.jens_mueller.jtcemu.platform.fx to javafx.web;
    opens org.jens_mueller.jtcemu.platform.fx;
}