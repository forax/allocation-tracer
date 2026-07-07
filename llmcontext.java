import module java.compiler;
import module jdk.compiler;
import javax.tools.ToolProvider;
import com.sun.source.tree.IdentifierTree;

void dependency(List<String> filenames, Path root, Set<Path> dependencies) throws IOException {
  var compiler = ToolProvider.getSystemJavaCompiler();

  var fileManager = compiler.getStandardFileManager(null, null, null);
  var diagnostics = new DiagnosticCollector<>();   // collect errors
  var options = List.of("-classpath", root.toString());
  var files = fileManager.getJavaFileObjectsFromStrings(filenames);

  var task = (JavacTask) compiler.getTask(null, fileManager, diagnostics, options, null, files);

  var asts = task.parse();
  task.analyze(); // Symbol resolution

  var trees = Trees.instance(task);

  for (var ast : asts) {
    new TreePathScanner<Void, Void>() {

      @Override
      public Void visitIdentifier(IdentifierTree node, Void p) {
        handle(node);
        return super.visitIdentifier(node, p);
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree node, Void p) {
        handle(node);
        return super.visitMemberSelect(node, p);
      }

      private static TypeElement getTopLevelType(TypeElement type) {
        var current = type;
        while (current.getEnclosingElement() instanceof TypeElement enclosing) {
          current = enclosing;
        }
        return current;
      }

      private void handle(Tree node) {
        var element = trees.getElement(getCurrentPath());

        if (element instanceof TypeElement typeElement) {
          var topLevel = getTopLevelType(typeElement);
          var fqName = topLevel.getQualifiedName().toString();

          if (fqName.startsWith("com.github.forax")) {
            var filename = root.resolve(fqName.replace('.', '/') + ".java");
            dependencies.add(filename);
          }
        }
      }

    }.scan(ast, null);
  }
}

void main(String[] args) throws IOException {
  var directories = List.of(Path.of("src/main/java"));
  var excludes = Set.of(
      "package-info.java"
  );
  var includes = Set.of(args);

  var removeDocComment = false;
  var removeComment = false;
  var removeImport = true;
  var removeSuper = true;
  var removeOverride = true;

  var output = Path.of("llmcontext.txt");
  var outputName = output.toString();

  var rootfiles = new ArrayList<String>();
  var otherfiles = new ArrayList<Path>();
  otherfiles.add(Path.of("pom.xml"));

  // Find all files in the project
  for(var directory : directories) {
    try (var files = Files.walk(directory)) {
      for (var file : (Iterable<Path>) files::iterator) {
        if (Files.isDirectory(file)) {
          continue;
        }
        var fileName = file.toString();
        if (fileName.startsWith("./target") ||   // skip maven "target"
            fileName.endsWith(outputName) || fileName.endsWith("llmcontext.java")) {   // skip our own files
          continue;
        }

        var shortFileName = file.getFileName().toString();
        if (!includes.isEmpty() && !includes.contains(shortFileName)) {
          //System.err.println("not included " + fileName);
          continue;
        }
        if (excludes.contains(shortFileName)) {
          System.err.println("exclude " + fileName);
          continue;
        }

        var contentType = Files.probeContentType(file);
        if (contentType == null || !contentType.startsWith("text/")) {
          System.err.println("exclude " + fileName);
          continue;
        }
        IO.println(fileName + " " + contentType);

        if (fileName.endsWith(".java")) {
          rootfiles.add(fileName);
        } else {
          otherfiles.add(file);
        }
      }
    }
  }

  // find all dependencies of the java files
  var dependencies = new HashSet<Path>();
  var processed = new HashSet<String>();
  var queue = new ArrayDeque<String>(rootfiles);

  var root = Path.of("./src/main/java");
  while (!queue.isEmpty()) {
    var file = queue.poll();
    if (!processed.add(file)) continue;

    var newDependencies = new HashSet<Path>();
    dependency(List.of(file), root, newDependencies);

    for (var newDependency : newDependencies) {
      if (dependencies.add(newDependency)) {
        queue.add(newDependency.toString());
      }
    }
  }

  // sort the files
  var files = Stream.concat(dependencies.stream(), otherfiles.stream())
      .sorted()
      .toList();

  try(var writer = Files.newBufferedWriter(output)) {
    for (var file : files) {
      var shortFileName = file.getFileName().toString();
      if (excludes.contains(shortFileName)) {
        System.err.println("exclude " + file);
        continue;
      }
      IO.println("file " + shortFileName);

      writer.write("-----\n");
      writer.write("file " + shortFileName);
      writer.write("\n---\n");
      try(var reader = Files.newBufferedReader(file)) {
        String line;
        while((line = reader.readLine()) != null) {
          if (line.isBlank()) {
            continue;   // skip empty lines
          }
          var strippedLine = line.strip();
          if (removeComment && strippedLine.startsWith("// ")) {
            continue;   // skip comments
          }
          if (removeDocComment && strippedLine.startsWith("///")) {
            continue;   // skip doc comments
          }
          if (removeImport && (strippedLine.startsWith("import") || strippedLine.startsWith("package"))) {
            continue;   // skip import and package
          }
          if (removeOverride && strippedLine.equals("@Override")) {
            continue;  // skip @Override
          }
          if (removeSuper && strippedLine.equals("super();")) {
            continue;  // skip super();
          }
          writer.write(line);
          writer.write("\n");
        }
      }
    }
  }
}