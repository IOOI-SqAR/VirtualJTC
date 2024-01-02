module VirtualJTCCommon {
    requires java.logging;
    requires java.naming;
    requires java.desktop;

    exports org.sqar.virtualjtc.jtcemu;
    opens org.sqar.virtualjtc.jtcemu;
}