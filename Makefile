JAVA = java
JCC = javac
JFLAGS = -g -d target/classes
JAR = jar
JARFLAGS = cvfm
JAVAFILES=$(shell git ls-files *.java)
CLASSES =$(shell find . -name \*.class)
TARGET=target

compile: resolve
	@echo "compiling..."
	mkdir -p $(TARGET)/classes
	$(JCC) $(JFLAGS) $(JAVAFILES)

resolve:
	$(JAVA) -jar $(IVY) -sync -retrieve "target/lib/[artifact]-[type].[ext]"

package: compile
	@echo "packaging..."
	$(JAR) $(JARFLAGS) target/pg_migrate.jar src/manifest $(CLASSES)

clean:
	@echo "cleaning..."
	rm -rf target

