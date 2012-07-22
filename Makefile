JAVA = java
JCC = javac
JFLAGS = -g -d target/classes
TESTJFLAGS = -g -d target/test-classes
JAR = jar
JARFLAGS = cvfm
TARGET=target
LIB=target/lib/default
TESTLIB=target/lib/test
JAVAFILES=$(shell find src -name \*.java)
TESTJAVAFILES=$(shell find test -name \*.java)
CLASSPATH =$(shell echo $$(JARS=("$(LIB)"/*.jar); IFS=:; echo "$${JARS[*]}"))
TESTCLASSPATH=$(shell echo $$(JARS=("$(TESTLIB)"/*.jar); IFS=:; echo "$${JARS[*]}"))
JAVADEPENDS=$(shell find $(LIB) -name *.jar)
JAVATESTDEPENDS=$(shell find $(TESTLIB) -name *.jar)
CLASSES =$(shell find target/classes -name \*.class)
TESTCLASSES =$(shell find target/test-classes -name \*.class)

compile: resolve
	@echo "compiling..."
	mkdir -p $(TARGET)/classes
	mkdir -p $(TARGET)/classes/io/pgmigrate/templates
	cp res/io/pgmigrate/templates/*.erb $(TARGET)/classes/io/pgmigrate/templates
	$(JCC) $(JFLAGS) -cp $(CLASSPATH) $(JAVAFILES)

testcompile: compile
	@echo "test compiling..."
	mkdir -p $(TARGET)/test-classes
	$(JCC) $(TESTJFLAGS) -cp $(TARGET)/classes:$(CLASSPATH):$(TESTCLASSPATH) $(TESTJAVAFILES)

resolve:
	$(JAVA) -jar $(IVY) -sync -retrieve "target/lib/[conf]/[artifact]-[type].[ext]"

package: compile
	@echo "packaging..."
	$(JAR) $(JARFLAGS) target/pg_migrate.jar src/manifest $(CLASSES)

test: testcompile
	@echo "testing..."
	mkdir -p $(TARGET)/test-output
	$(JAVA) -cp $(TARGET)/classes:$(TARGET)/test-classes:$(CLASSPATH):$(TESTCLASSPATH) org.testng.TestNG test/testng.xml

clean:
	@echo "cleaning..."
	rm -rf target

