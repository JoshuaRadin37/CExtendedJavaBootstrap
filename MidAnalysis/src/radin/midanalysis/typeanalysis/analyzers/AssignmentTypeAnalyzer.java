package radin.midanalysis.typeanalysis.analyzers;

import radin.midanalysis.TypeAugmentedSemanticNode;
import radin.output.tags.BasicCompilationTag;
import radin.output.typeanalysis.errors.ConstModificationError;
import radin.core.lexical.Token;
import radin.core.lexical.TokenType;
import radin.core.semantics.ASTNodeType;
import radin.core.semantics.types.CXType;
import radin.core.semantics.types.wrapped.ConstantType;
import radin.core.semantics.types.primitives.CXPrimitiveType;
import radin.output.typeanalysis.TypeAnalyzer;
import radin.output.typeanalysis.errors.IllegalLValueError;
import radin.output.typeanalysis.errors.IncorrectTypeError;
import radin.core.utility.ICompilationSettings;

public class AssignmentTypeAnalyzer extends TypeAnalyzer {
    
    public AssignmentTypeAnalyzer(TypeAugmentedSemanticNode tree) {
        super(tree);
    }
    
    @Override
    public boolean determineTypes(TypeAugmentedSemanticNode node) {
        assert node.getASTType() == ASTNodeType.assignment;
        
        TypeAugmentedSemanticNode lhs = node.getChild(0);
        TypeAugmentedSemanticNode rhs = node.getChild(2);
        
        TypeAnalyzer analyzer = new ExpressionTypeAnalyzer(lhs);
        if (!determineTypes(analyzer)) {
            return false;
        }
    
    
        ICompilationSettings.debugLog.finest("Determining validity of assigning to a " + lhs.getCXType());
        if(lhs.getCXType() instanceof ConstantType || lhs.getCXType().isStrictlyArray()) {
            
            if(lhs.getASTType() == ASTNodeType.id)
                throw new ConstModificationError(lhs.getToken().getImage(), lhs.getCXType());
            else {
                throw new ConstModificationError(lhs.getCXType());
            }
        }
        
        
        
        CXType rhsType;
        if(rhs.getASTType() != ASTNodeType.assignment) {
            analyzer = new ExpressionTypeAnalyzer(rhs);
            if (!determineTypes(analyzer)) {
                return false;
            }
            rhsType = rhs.getCXType();
        }else {
            if(!determineTypes(rhs)) return false;
            rhsType = rhs.getChild(0).getCXType();
        }
        
        if(!lhs.isLValue()) throw new IllegalLValueError(node.findFirstToken().getPrevious());
    
        Token operator = node.getASTChild(ASTNodeType.assignment_type).getToken();
        if(operator.getType() != TokenType.t_assign) {
            node.addCompilationTag(BasicCompilationTag.OPERATOR_ASSIGNMENT);
        }
        
        if(operator.getType() == TokenType.t_assign) {
            if(rhs.getToken() != null && rhs.getToken().getImage() != null && rhs.getToken().getImage().equals(
                    "nullptr")) {
              ICompilationSettings.debugLog.finer("Assigning to nullptr bypasses typesystem");
            } else if(!is(rhsType, lhs.getCXType())) {
                setIsFailurePoint(rhs);
                throw new IncorrectTypeError(lhs.getCXType(), rhsType, lhs.findFirstToken(), rhs.findFirstToken());
            }
            //if(!rhsType.is(lhs.getCXType(), getEnvironment())) throw new IncorrectTypeError(lhs.getCXType(), rhsType);
            
        } else if(operator.getType() == TokenType.t_operator_assign) {
        
            // TODO: IMPLEMENT OPERATOR ASSIGN
        } else throw new IllegalArgumentException();
        
        node.setType(CXPrimitiveType.VOID);
        return true;
    }
}
