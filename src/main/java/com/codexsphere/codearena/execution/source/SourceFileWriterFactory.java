package com.codexsphere.codearena.execution.source;

import com.codexsphere.codearena.enums.Language;
import com.codexsphere.codearena.execution.source.strategy.CppSourceFileWriter;
import com.codexsphere.codearena.execution.source.strategy.JavaScriptSourceFileWriter;
import com.codexsphere.codearena.execution.source.strategy.JavaSourceFileWriter;
import com.codexsphere.codearena.execution.source.strategy.PythonSourceFileWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SourceFileWriterFactory {

    private final JavaSourceFileWriter javaWriter;
    private final CppSourceFileWriter cppSourceFileWriter;
    private final PythonSourceFileWriter pythonSourceFileWriter;
    private final JavaScriptSourceFileWriter javaScriptSourceFileWriter;

    public SourceFileWriter getWriter(Language language){

        return switch (language){

            case JAVA -> javaWriter;

            case CPP -> cppSourceFileWriter;

            case PYTHON -> pythonSourceFileWriter;

            case JAVASCRIPT -> javaScriptSourceFileWriter;

        };

    }

}