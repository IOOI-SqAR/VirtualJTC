module org.jens_mueller.JTCEMUCommon {
    exports org.jens_mueller.jtcemu.base to org.jens_mueller.jtcemu.platform.se, org.jens_mueller.jtcemu.platform.fx;
    exports org.jens_mueller.jtcemu.tools to org.jens_mueller.jtcemu.platform.se, org.jens_mueller.jtcemu.platform.fx;
    exports org.jens_mueller.jtcemu.tools.assembler to org.jens_mueller.jtcemu.platform.se, org.jens_mueller.jtcemu.platform.fx;
    exports org.jens_mueller.z8 to org.jens_mueller.jtcemu.platform.se, org.jens_mueller.jtcemu.platform.fx;

    opens org.jens_mueller.jtcemu.base to org.jens_mueller.jtcemu.platform.se, org.jens_mueller.jtcemu.platform.fx;
    opens org.jens_mueller.jtcemu.tools to org.jens_mueller.jtcemu.platform.se, org.jens_mueller.jtcemu.platform.fx;
    opens org.jens_mueller.jtcemu.tools.assembler to org.jens_mueller.jtcemu.platform.se, org.jens_mueller.jtcemu.platform.fx;
    opens org.jens_mueller.z8 to org.jens_mueller.jtcemu.platform.se, org.jens_mueller.jtcemu.platform.fx;
}