module JTCemuFX {
    requires javafx.web;

    requires JTCEMUCommon;

    opens org.jens_mueller.jtcemu.platform.fx;
    exports org.jens_mueller.jtcemu.platform.fx;
}