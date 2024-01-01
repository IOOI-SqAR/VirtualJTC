module org.jens_mueller.jtcemu.platform.se {
    requires java.logging;
    requires java.naming;
    requires java.desktop;

    requires org.jens_mueller.JTCEMUCommon;

    opens org.jens_mueller.jtcemu.platform.se;
    exports org.jens_mueller.jtcemu.platform.se;
}