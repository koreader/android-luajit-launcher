apk:
	android update project --path . -t android-19
	./copyEPDControllers.sh
	ant debug
	cp -pv bin/EinkTest-debug.apk einkTest.apk
clean:
	rm -rf bin gen libs local.properties project.properties proguard-project.txt einkTest.apk
	rm -rf src/org/koreader/einkTest/*EPDController.java

