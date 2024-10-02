# Java CV Image Resize Demo

## Run 

```
./gradlew clean build

java -Xms512M -Xmx1024M \
-Dorg.bytedeco.javacpp.maxBytes=1000M \
-Dorg.bytedeco.javacpp.maxPhysicalBytes=2000M \
-Dorg.bytedeco.javacpp.nopointergc=true \
-jar app.jar
```