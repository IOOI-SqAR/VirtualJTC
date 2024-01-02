module JTCEMU {
    requires java.logging;
    requires java.naming;
    requires java.desktop;

    requires JTCEMUCommon;

    opens org.jens_mueller.jtcemu.platform.se;
    exports org.jens_mueller.jtcemu.platform.se;
}