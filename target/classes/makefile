# 1. DETECT THE OPERATING SYSTEM
OS := $(shell uname -s 2>/dev/null || echo Windows)

# 2. ROUTE TO THE CORRECT JAVAFX FOLDER BASED ON YOUR DIRECTORY
ifeq ($(OS),Windows)
	PATH_TO_FX = ../javafx-windows-26.0.2/lib
else ifeq ($(OS),Darwin)
	PATH_TO_FX = ../javafx-mac/lib
else
	PATH_TO_FX = ../javafx-linux/lib
endif

# 3. BUILD AND RUN COMMANDS
all: run

# (FIXED) Removed the native-access flag from the compiler
compile:
	javac --module-path $(PATH_TO_FX) --add-modules javafx.controls,javafx.fxml,javafx.graphics algorithm/*.java frontend/*.java

# (KEPT) The native-access flag remains here for the runtime launch
run: compile
	java --enable-native-access=javafx.graphics --module-path $(PATH_TO_FX) --add-modules javafx.controls,javafx.fxml,javafx.graphics frontend.Launcher

clean:
	rm -f *.class
	rm -f algorithm/*.class
	rm -f frontend/*.class
	rm -f db/*.class
	@echo "Workspace cleaned: All .class files have been removed."