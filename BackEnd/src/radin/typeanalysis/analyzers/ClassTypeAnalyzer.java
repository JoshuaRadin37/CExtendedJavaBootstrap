package radin.typeanalysis.analyzers;

import radin.interphase.lexical.Token;
import radin.interphase.semantics.ASTNodeType;
import radin.interphase.semantics.AbstractSyntaxNode;
import radin.interphase.semantics.types.CXType;
import radin.interphase.semantics.types.TypeAbstractSyntaxNode;
import radin.interphase.semantics.types.Visibility;
import radin.interphase.semantics.types.compound.CXClassType;
import radin.interphase.semantics.types.compound.FunctionPointer;
import radin.interphase.semantics.types.methods.ParameterTypeList;
import radin.typeanalysis.TypeAnalyzer;
import radin.typeanalysis.TypeAugmentedSemanticNode;

import java.util.LinkedList;
import java.util.List;

public class ClassTypeAnalyzer extends TypeAnalyzer {
    
    public ClassTypeAnalyzer(TypeAugmentedSemanticNode tree) {
        super(tree);
    }
    
    @Override
    public boolean determineTypes(TypeAugmentedSemanticNode node) {
        assert node.getASTNode() instanceof TypeAbstractSyntaxNode;
        assert ((TypeAbstractSyntaxNode) node.getASTNode()).getCxType() instanceof CXClassType;
        CXClassType cxClassType = (CXClassType) ((TypeAbstractSyntaxNode) node.getASTNode()).getCxType();
        
        typeTrackingClosure(cxClassType);
    
    
        List<TypeAugmentedSemanticNode> decs = node.getAllChildren(ASTNodeType.class_level_declaration);
        // STEP 1 -> set up fields and methods in tracker
        for (TypeAugmentedSemanticNode clsLevelDec : decs) {
            
    
            Visibility visibility =
                    Visibility.getVisibility(clsLevelDec.getASTChild(ASTNodeType.visibility).getToken());
            
            if(clsLevelDec.hasASTChild(ASTNodeType.declarations)) {
    
                TypeAugmentedSemanticNode fields = clsLevelDec.getASTChild(ASTNodeType.declarations);
                DeclarationsAnalyzer fieldAnalyzers = new DeclarationsAnalyzer(
                        fields,
                        ASTNodeType.declaration,
                        cxClassType,
                        visibility
                );
                
                if(!determineTypes(fieldAnalyzers)) return false;
            } else if(clsLevelDec.hasASTChild(ASTNodeType.function_definition)) {
                assert clsLevelDec.getASTChild(ASTNodeType.function_definition).getASTNode() instanceof TypeAbstractSyntaxNode;
                TypeAbstractSyntaxNode astNode =
                        ((TypeAbstractSyntaxNode) clsLevelDec.getASTChild(ASTNodeType.function_definition).getASTNode());
    
                CXType returnType = astNode.getCxType();
                String name = astNode.getChild(ASTNodeType.id).getToken().getImage();
                List<CXType> parameterTypes = new LinkedList<>();
                for (AbstractSyntaxNode abstractSyntaxNode : astNode.getChild(ASTNodeType.parameter_list)) {
                    assert  abstractSyntaxNode instanceof TypeAbstractSyntaxNode;
                    CXType paramType = ((TypeAbstractSyntaxNode) abstractSyntaxNode).getCxType().getTypeRedirection(getEnvironment());
                    parameterTypes.add(paramType);
                }
    
                FunctionPointer type = new FunctionPointer(returnType,
                        parameterTypes);
                clsLevelDec.getASTChild(ASTNodeType.function_definition).setType(type);
    
                assert visibility != null;
                switch (visibility) {
                    case _public: {
                        getCurrentTracker().addPublicMethod(cxClassType,  name, returnType, type.getParameterTypeList());
                        break;
                    }
                    case internal: {
                        getCurrentTracker().addInternalMethod(cxClassType, name, returnType, type.getParameterTypeList());
                        break;
                    }
                    case _private: {
                        getCurrentTracker().addPrivateMethod(cxClassType, name, returnType, type.getParameterTypeList());
                        break;
                    }
                }
                
                
            }
        }
        
        //STEP 2 -> analyze functions
        List<TypeAugmentedSemanticNode> functions = node.getAllChildren(ASTNodeType.function_definition);
        for (TypeAugmentedSemanticNode function : functions) {
            FunctionTypeAnalyzer analyzer = new FunctionTypeAnalyzer(function, cxClassType);
            
            if(!determineTypes(analyzer)) return false;
        }
    
        releaseTrackingClosure();
        return true;
    }
}
