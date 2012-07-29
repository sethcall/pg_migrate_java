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
TESTCLASSES =$(shell find target/test-classes -name \*.class)
JARCLASSPATH =$(shell cd target; find depends -name \*.jar)
compile: resolve
	@echo "compiling..."
	@mkdir -p $(TARGET)/classes
	@mkdir -p $(TARGET)/classes/io/pgmigrate/templates
	@mkdir -p $(TARGET)/classes/io/pgmigrate/packaging/templates
	@# copy resources
	@cp res/io/pgmigrate/templates/*.erb $(TARGET)/classes/io/pgmigrate/templates
	@cp res/io/pgmigrate/logback* $(TARGET)/classes/io/pgmigrate
	@cp res/io/pgmigrate/packaging/templates/*.vm $(TARGET)/classes/io/pgmigrate/packaging/templates
	@# copy dependencies
	@mkdir -p $(TARGET)/depends
	@cp -r $(LIB)/. $(TARGET)/depends
	$(JCC) $(JFLAGS) -cp $(CLASSPATH) $(JAVAFILES)

testcompile: compile
	@echo "test compiling..."
	@mkdir -p $(TARGET)/test-classes
	@cp -r test/input_manifests $(TARGET)/test-classes
	$(JCC) $(TESTJFLAGS) -cp $(TARGET)/classes:$(CLASSPATH):$(TESTCLASSPATH) $(TESTJAVAFILES)

resolve:
	@echo "resolving dependencies..."
	$(JAVA) -jar $(IVY) -sync -retrieve "target/lib/[conf]/[artifact]-[type].[ext]"

package: test
	@echo "packaging jar..."
	@cp src/manifest target/manifest
	@echo "Class-Path: $(JARCLASSPATH)" >> target/manifest
	@echo "" >> target/manifest
	cd target/classes; $(JAR) $(JARFLAGS) ../pg_migrate.jar ../manifest `find . -name \*.class -or -name \*.erb -or -name \*.xml -or -name \*.vm`

test: testcompile
	@echo "testing..."
	mkdir -p $(TARGET)/test-output
	$(JAVA) -cp $(TARGET)/classes:$(TARGET)/test-classes:$(CLASSPATH):$(TESTCLASSPATH) org.testng.TestNG test/testng.xml -d $(TARGET)/test-output

clean:
	@echo "cleaning..."
	rm -rf target

