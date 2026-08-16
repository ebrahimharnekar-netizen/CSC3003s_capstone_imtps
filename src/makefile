# Default target to compile and run
all: run

# Compile the main file and the algorithm classes
compile:
	javac imtps.java algorithm/*.java

# Run the compiled program
run: compile
	java imtps

# Clean up the directory by deleting all .class files
clean:
	rm -f *.class
	rm -f algorithm/*.class
	@echo "Workspace cleaned: All .class files have been removed."