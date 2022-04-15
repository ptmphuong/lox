package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }
    String outputDir = args[0];

    defineAst(
        outputDir,
        "Expr",
        Arrays.asList(
            "Binary   : Expr left, Token operator, Expr right",
            "Grouping : Expr expression",
            "Literal  : Object value",
            "Unary    : Token operator, Expr right"
        )
    );

    System.out.println("Written to dir: " + outputDir);
  }


  private static void defineAst(
      String outputDir, String baseName, List<String> types)
      throws IOException {
    String path = outputDir + "/" + baseName + ".java";
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    writer.println("package com.craftinginterpreters.lox;");
    writer.println();
    writer.println("import com.craftinginterpreters.lox.Token;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();
    writer.println("abstract class " + baseName + " {");

    // define Visitor interface
    writer.println();
    writer.println("  abstract <R> R accept(Visitor<R> visitor);");
    defineVisitor(writer, baseName, types);
    writer.println();

    for (String type: types) {
      String className = type.split(":")[0].trim();
      String fields = type.split(":")[1].trim();
      defineType(writer, baseName, className, fields);
      writer.println();
    }

    writer.println("}");
    writer.close();
  }


  private static void defineVisitor(
      PrintWriter writer, String baseName, List<String> types
  ) {
    writer.println("  interface Visitor<R> {");

    for (String type: types) {
      String typename = type.split(":")[0].trim();
      String visit = String.format(
          "    R visit%s%s(%s %s);",
          typename, baseName, typename, baseName.toLowerCase()
      );
      writer.println(visit);
    }

    writer.println("  }");
  }


  private static void defineType(
      PrintWriter writer, String baseName, String className, String fieldList
  ) {
    String classDeclaration = String.format(
        "  static class %s extends %s {",
        className, baseName
        );
    writer.println(classDeclaration);

    String[] fields = fieldList.split(", ");

    // declare fields
    for (String field: fields) {
      String fieldDeclaration = String.format("    final %s;", field);
      writer.println(fieldDeclaration);
    }

    // constructor
    String constructorSignature = String.format("    %s (%s) {", className, fieldList);
    writer.println(constructorSignature);
    for (String field: fields) {
      String name = field.split(" ")[1];
      String fieldInitialization = String.format("      this.%s = %s;", name, name);
      writer.println(fieldInitialization);
    }

    writer.println("      }");

    writer.println();
    writer.println("    @Override");
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    writer.println("      return visitor.visit" +
        className + baseName + "(this);");
    writer.println("    }");

    writer.println("    }");
  }

}
