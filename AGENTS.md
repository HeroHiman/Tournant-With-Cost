# BMAD Execution Rules (Strict Verification)

Whenever you are assigned a Story or a file to fix, you MUST follow this sequence. Do not skip steps.

1. **PRE-FLIGHT READ:** Before writing any code, you must read the entire target file using your file-read tool. Identify existing imports, interfaces, and variable scopes.
2. **ISOLATED WRITE:** Apply the changes. If you use a new class or component (like `Setting`), verify that the import statement is added at the top of the file.
3. **SYNTAX CHECK:** Check your own work for unclosed quotes, brackets, or missing types before declaring the step finished.
4. **POST-FLIGHT COMPILE:** After modifying the file, run the local linter or compiler (e.g., `npx tsc --noEmit`, `npm run lint`, `./gradlew compileDebugSources`, or project equivalent) to prove your changes did not introduce new errors. If it fails, fix your own errors automatically.
