all: world

JAVAPATH = environment/wumpuslite/WumpusWorld/src/main/java/wumpusworld/
COMMITPATH = environment/
DIR = $(shell pwd)

world:
	javac $(JAVAPATH)*.java
	echo Environment compiled 

clean:
	rm $(JAVAPATH)*.class

run: world
	java -classpath $(JAVAPATH) WorldApplication > /dev/null

javacp:
	cp $(JAVAPATH)*.java $(COMMITPATH)