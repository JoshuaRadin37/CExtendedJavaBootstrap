package radin.core.frontend;

import radin.core.AbstractTree;
import radin.core.errorhandling.AbstractCompilationError;
import radin.core.errorhandling.ICompilationErrorCollector;

import java.util.LinkedList;
import java.util.List;

public class FrontEndUnit<T, P extends AbstractTree<? extends P>, S> implements ICompilationErrorCollector {
    ITokenizer<? extends T> lexer;
    IParser<? super T, ? extends P> parser;
    ISemanticAnalyzer<? super P, ? extends S> builder;
    
    public FrontEndUnit(ITokenizer<? extends T> lexer, IParser<? super T, ? extends P> parser, ISemanticAnalyzer<? super P, ? extends S> builder) {
        this.lexer = lexer;
        this.parser = parser;
        this.parser.setTokenizer(lexer);
        this.builder = builder;
    }
    
    public S build() {
        lexer.run();
        if(lexer.hasErrors()) return null;
        P parse = parser.parse();
        if(parse == null || parser.hasErrors()) {
            return null;
        }
        parse.printTreeForm();
        return builder.analyze(parse);
    }
    
    @Override
    public List<AbstractCompilationError> getErrors() {
        List<AbstractCompilationError> output = new LinkedList<>();
        if(lexer.hasErrors()) {
            output.addAll(lexer.getErrors());
        }
        if(parser.hasErrors()) {
            output.addAll(parser.getErrors());
        }
        if(builder.hasErrors()) {
            output.addAll(builder.getErrors());
        }
        
        return output;
    }
}