package radin.typeanalysis.analyzers;

import radin.interphase.semantics.ASTNodeType;
import radin.interphase.semantics.AbstractSyntaxNode;
import radin.interphase.semantics.types.CXType;
import radin.interphase.semantics.types.PointerType;
import radin.interphase.semantics.types.TypeAbstractSyntaxNode;
import radin.interphase.semantics.types.Visibility;
import radin.interphase.semantics.types.compound.CXClassType;
import radin.interphase.semantics.types.compound.CXFunctionPointer;
import radin.interphase.semantics.types.compound.CXStructType;
import radin.interphase.semantics.types.methods.CXMethod;
import radin.interphase.semantics.types.methods.CXParameter;
import radin.interphase.semantics.types.methods.ParameterTypeList;
import radin.interphase.semantics.types.primitives.CXPrimitiveType;
import radin.typeanalysis.TypeAnalyzer;
import radin.typeanalysis.TypeAugmentedSemanticNode;
import radin.typeanalysis.TypeAugmentedSemanticTree;
import radin.typeanalysis.errors.IncorrectReturnTypeError;
import radin.typeanalysis.errors.RedeclarationError;

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
        cxClassType.generateSuperMethods(getCompilationSettings().getvTableName());
        for (CXMethod generatedSuper : cxClassType.getGeneratedSupers()) {
            typeTrackingClosure();
        
            CXStructType vTable = cxClassType.getVTable();
            getCurrentTracker().addBasicCompoundType(vTable);
            getCurrentTracker().addPrivateField(cxClassType, getCompilationSettings().getvTableName(),
                    vTable);
        
            getCurrentTracker().addVariable("__this", new PointerType(CXPrimitiveType.VOID));
        
        
        
            for (CXParameter parameter : generatedSuper.getParameters()) {
                getCurrentTracker().addVariable(parameter.getName(), parameter.getType());
            }
            TypeAugmentedSemanticNode tree = new TypeAugmentedSemanticTree(generatedSuper.getMethodBody(),
                    getEnvironment()).getHead();
            CompoundStatementTypeAnalyzer compoundStatementTypeAnalyzer = new CompoundStatementTypeAnalyzer(tree,
                    generatedSuper.getReturnType(), false);
            tree.printTreeForm();
            /* // TODO: implement type checking for super methods. Currently have to be non type checked
            if(!determineTypes(compoundStatementTypeAnalyzer)) {
                tree.printTreeForm();
                return false;
            }
            
             */
            getMethods().put(generatedSuper, tree);
            releaseTrackingClosure();
        }
    
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
                
                
    
                CXFunctionPointer type = new CXFunctionPointer(returnType,
                        parameterTypes);
                clsLevelDec.getASTChild(ASTNodeType.function_definition).setType(type);
    
                boolean isVirtual  = astNode.hasChild(ASTNodeType._virtual);
                ParameterTypeList parameterTypeList = type.getParameterTypeList();
                if(isVirtual && getCurrentTracker().methodVisible(cxClassType, name, parameterTypeList)) {
                    CXType virtType = getCurrentTracker().getMethodType(cxClassType, name, parameterTypeList);
                    if(!is(returnType, virtType)) {
                        
                        throw new IncorrectReturnTypeError(virtType, returnType);
                    }
                } else {
    
    
                    assert visibility != null;
    
                    switch (visibility) {
                        case _public: {
                            getCurrentTracker().addPublicMethod(cxClassType, name, returnType,
                                    parameterTypeList);
                            break;
                        }
                        case internal: {
                            getCurrentTracker().addInternalMethod(cxClassType, name, returnType, parameterTypeList);
                            break;
                        }
                        case _private: {
                            getCurrentTracker().addPrivateMethod(cxClassType, name, returnType, parameterTypeList);
                            break;
                        }
                    }
                }
                
            } else if(clsLevelDec.hasASTChild(ASTNodeType.constructor_definition)) {
                TypeAugmentedSemanticNode def = clsLevelDec.getASTChild(ASTNodeType.constructor_definition);
    
                List<CXType> parameterTypes = new LinkedList<>();
                for (AbstractSyntaxNode abstractSyntaxNode :
                        def.getASTChild(ASTNodeType.parameter_list).getASTNode().getChildList()) {
                    assert  abstractSyntaxNode instanceof TypeAbstractSyntaxNode;
                    CXType paramType = ((TypeAbstractSyntaxNode) abstractSyntaxNode).getCxType().getTypeRedirection(getEnvironment());
                    parameterTypes.add(paramType);
                }
                
                ParameterTypeList typeList = new ParameterTypeList(parameterTypes);
                
                if(getCurrentTracker().constructorVisible(cxClassType, typeList)) {
                    throw new RedeclarationError("constructor for " + cxClassType+ " with parameters" + parameterTypes);
                }
                
                getCurrentTracker().addConstructor(visibility, cxClassType, typeList);
            
            }
        }
        
        //STEP 2 -> analyze functions and constructors
        List<TypeAugmentedSemanticNode> functions = node.getAllChildren(ASTNodeType.function_definition);
        for (TypeAugmentedSemanticNode function : functions) {
            FunctionTypeAnalyzer analyzer = new FunctionTypeAnalyzer(function, cxClassType);
            
            if(!determineTypes(analyzer)) return false;
        }
    
        List<TypeAugmentedSemanticNode> constructors = node.getAllChildren(ASTNodeType.constructor_definition);
        for (TypeAugmentedSemanticNode constructor : constructors) {
            ConstructorTypeAnalyzer analyzer = new ConstructorTypeAnalyzer(constructor, cxClassType);
            
            if(!determineTypes(analyzer)) return false;
        }
        
        
    
        releaseTrackingClosure();
        return true;
    }
}
