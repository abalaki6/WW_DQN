all: world

DIR = $(shell pwd)

JAVAPATH = $(DIR)/environment/wumpuslite/WumpusWorld/src/main/java/wumpusworld/
COMMITPATH = $(DIR)/environment/

JAVAFILES = $(shell ls -d -1 $(JAVAPATH)*.java*)
CLASSFILES = $(shell ls -d -1 $(JAVAPATH)*.java* | sed -e 's/.java/.class/g') 

world: $(JAVAFILES)
	javac $(JAVAPATH)*.java
	echo Environment compiled 

clean:
	rm $(JAVAPATH)*.class

run: world
	java -classpath $(JAVAPATH) WorldApplication > /dev/null

javacp:
	cp $(JAVAPATH)*.java $(COMMITPATH)

javarm:
	rm $(COMMITPATH)*.java