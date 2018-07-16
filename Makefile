JAVA=javac

all: msend mreceive

msend: msend.java
	$(JAVA) msend.java 

mreceive: mreceive.java
	$(JAVA) mreceive.java
