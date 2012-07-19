JCC = javac
JFLAGS = -g -d target/classes
JAR = jar
JARFLAGS = cvfm
JAVAFILES=$(shell git ls-files *.java)
CLASSES =$(shell find . -name \*.class)
TARGET=target

compile:
	@echo "compiling..."
	mkdir -p $(TARGET)/classes
	$(JCC) $(JFLAGS) $(JAVAFILES)

package: compile
	@echo "packaging..."
	$(JAR) $(JARFLAGS) target/pg_migrate.jar src/manifest $(CLASSES)

clean:
	@echo "cleaning..."
	rm -rf target

