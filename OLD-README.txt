JTCEMU
======


1. Allgemeines
--------------

JTCEMU ist ein Software-Emulator, der den von Dr. Helmut Hoyer ab 1987
in der Zeitschrift Jugend+Technik veroeffentlichten
Eigenbaucomputer nachbildet.


2. Lizenzbestimmungen und Haftung
---------------------------------

Die Software JTCEMU darf von jedermann, auch fuer kommerzielle Zwecke,
entsprechend den Bedingungen der GNU General Puplic License (GNU-GPL)
Version 3 angewendet und weitergegeben werden.
Weitere Rechte, die in der GNU-GPL nicht aufgefuehrt sind,
werden ausdruecklich nicht gewaehrt.
Wird das Programm weiterentwickelt oder der Quelltext teilweise
oder vollstaendig in anderen Programmen verwendet,
so ist die daraus neu entstandene Software ebenfalls unter der GNU-GPL
zu veroeffentlichen und muss einen Copyright-Vermerk mit allen,
auch den urspruenglichen, Rechteinhabern enthalten.
Den originalen Wortlaut der GNU-GPL finden Sie in der Datei LICENSE.txt.
Sollten Sie Verstaendnisprobleme mit dem englischsprachigen Wortlaut haben,
muessen Sie sich selbst um eine juristisch abgesicherte Uebersetzung bemuehen,
bevor Sie JTCEMU anwenden, weitergeben oder modifizieren.
Der Autor stellt die Software entsprechend den Bedingungen der GNU-GPL
zur Verfuegung.
Der Gebrauch der Software ist kostenfrei und erfolgt deshalb ausschliesslich
auf eigenes Risiko!

Jegliche Gewaehrleistung und Haftung ist ausgeschlossen!


3. Von der GNU-GPL ausgenommene Programmteile
---------------------------------------------

Fuer den Betrieb von JTCEMU sind ROM-Images notwendig.
Diese finden Sie im Verzeichnis src/rom.
Die ROM-Images unterliegen nicht der GNU-GPL,
d.h., die Rechte, die Ihnen von der GNU-GPL bzgl. der Benutzung,
Modifizierung und Weitergabe von JTCEMU eingeraeumt werden,
gelten nicht fuer die ROM-Inhalte!
Jegliche Benutzung dieser ROM-Images ausserhalb von JTCEMU
oder ausserhalb eines rein privaten, nicht kommerziellen Umfeldes
muessen Sie im Zweifelsfall mit den Urhebern klaeren.

Die Urheberschaften der ROM-Inhalte liegen bei:
- VEB Mikroelektronik Erfurt (Tiny-MP-BASIC-Interpreter)
- Dr. Helmut Hoyer (2K-System und EMR-ES 1988)
- Harun Scheutzow (ES 2.3 und ES 4.0)


4. Installation
---------------

1. Installieren Sie die Java Runtime Environment (JRE)
   der Java Standard Edition (Java SE) Version 6 oder hoeher.
   Ueberpruefen Sie die installierte Java-Version durch den
   Kommandozeilenaufruf: "java -version"
   Fuer Java 6 muss die Versionsnummer 1.6.x erscheinen.
2. Laden Sie die Datei jtcemu-1.1.jar von http://www.jens-mueller.org/jtcemu
   herunter. Die Datei darf dabei nicht als ZIP-Datei entpackt werden!
3. Kopieren Sie die Datei jtcemu-1.1.jar in ein beliebiges Verzeichnis
   auf die Festplatte ihres Computers,
   z.B. unter Linux/Unix nach "/tmp" oder unter Windows nach "C:\".
4. Starten Sie JTCEMU
   - unter Linux/Unix mit: "java -jar /tmp/jtcemu-1.1.jar"
   - unter Windows in der DOS-Box mit: "javaw.exe -jar C:\jtcemu-1.1.jar"
5. Legen Sie auf dem Desktop eine "Verknuepfung zu einem Programm" an,
   und tragen dort die in der Console/DOS-Box erfolgreich verwendete
   Kommandozeile ein.


5. Compilieren
--------------

Moechten Sie den Quelltext compilieren, sind folgende Schritte notwendig:
1. Stellen Sie sicher, dass das Java Development Kit (JDK)
   der Java Standard Edition (Java SE) Version 6 oder hoeher installiert ist.
2. Wechseln Sie in das cmd-Verzeichnis des JTCEMU-Quelltextes
3. Compilieren Sie
   - unter Linux/Unix mit: "./compile"
   - unter Windows in der DOS-Box mit: "compile.cmd"

Alternativ koennen Sie auch mit "ant" compilieren.
Die dazu notwendige Datei build.xml finden Sie im Wurzelverzeichnis
des Quelltextes.


6. Dank
-------
Die Entwicklung des Emulators war nur moeglich durch die Zuarbeit anderer,
vorallem durch Bereitstellung von Dokumenten und Software.
Besonders bedanken moechte ich mich bei:

- Dr. Helmut Hoyer fuer die Entwicklung des Jugend+Technik-Computers
  und den dazu veroeffentlichten Programmen
- Harun Scheutzow fuer die freundliche Genehmigung zur Integration
  seiner Betriebssysteme in JTCEMU
- Volker Pohlers fuer die vielen Unterlagen,
  die er mir zur Verfuegung gestellt hat und fuer das intensive Testen
- Daniel Weitendorf fuer die Hilfe bei der Emulation des 2K-Kassettenformats
- Alexander Schoen fuer die Anfertigung eines WAV-Datei-Abzugs
  der Begleitkassette zum Sonderdruck zum ES 4.0
- allen, die den Emulator getestet haben


7. Kontakt
----------

Autor:  Jens Mueller
E-Mail: info@jens-mueller.org

Ihre Mail muss im Betreff das Wort "JTCEMU" enthalten.
Anderenfalls wird sie moeglicherweise vom Spam-Filter zurueckgehalten.

