# ExtendedModulePlayerJava
Java Wrapper for Extended Module Player 4.3 library

This library uses native libxmp library, you should compile it from sources before using this wrapper.
http://xmp.sourceforge.net/

Sample usage:

```
SourceDataLine line = AudioSystem.getSourceDataLine(new AudioFormat(44100, 16, 2, true, false));
line.open();
line.start();

Xmp xmp = new Xmp();
xmp.loadModule("d:/modules/ninja starts school.xm");
xmp.startPlayer(44100);

byte[] buffer = new byte[8192];
while (xmp.playBuffer(buffer, 1)) {
    line.write(buffer, 0, buffer.length);
}

line.stop();
line.close();
```
