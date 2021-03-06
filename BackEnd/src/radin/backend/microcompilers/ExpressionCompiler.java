package radin.backend.microcompilers;

import radin.backend.compilation.AbstractCompiler;
import radin.midanalysis.TypeAugmentedSemanticNode;
import radin.output.tags.BasicCompilationTag;
import radin.output.tags.ConstructorCallTag;
import radin.output.tags.MethodCallTag;
import radin.output.tags.SuperCallTag;
import radin.core.lexical.Token;
import radin.core.semantics.ASTNodeType;
import radin.core.semantics.TypeEnvironment;
import radin.core.semantics.types.CXType;
import radin.core.semantics.types.TypedAbstractSyntaxNode;
import radin.core.semantics.types.compound.CXClassType;
import radin.core.semantics.types.compound.CXFunctionPointer;
import radin.core.semantics.types.methods.CXMethod;
import radin.core.semantics.types.primitives.ArrayType;
import radin.core.semantics.types.primitives.PointerType;
import radin.core.utility.UniversalCompilerSettings;

import java.io.PrintWriter;

public class ExpressionCompiler extends AbstractCompiler {
    
    
    
    public ExpressionCompiler(PrintWriter printWriter) {
        super(printWriter);
    }
    
    @Override
    public boolean compile(TypeAugmentedSemanticNode node) {
        
        switch (node.getASTType()) {
            case string: {
                if(UniversalCompilerSettings.getInstance().getSettings().autoCreateStrings()) {
                    // TODO: Create auto string creation
                    break;
                } // else go to the same process as ids and literals
            }
            case id:
            case literal: {
                print(node.getToken().getImage());
                break;
            }
            case _true: {
                print("(!(bool) 0)");
                break;
            }
            case _false: {
                print("((bool) 0)");
                break;
            }
            case sizeof: {
                CXType type = ((TypedAbstractSyntaxNode) node.getASTNode()).getCxType();
                print("sizeof(");
                if(UniversalCompilerSettings.getInstance().getSettings().isInRuntimeCompilationMode() && type instanceof PointerType && ((PointerType) type).getSubType() instanceof CXClassType) {
                    print(((PointerType) type).getSubType().generateCDeclaration());
                } else {
                    print(type.generateCDefinition());
                }
                print(")");
                break;
            }
            case typeid: {
                node.setFailurePoint(true );
                return false;
            }
            case uniop: {
                Token opToken = node.getASTChild(ASTNodeType.operator).getToken();
                TypeAugmentedSemanticNode child = node.getChild(1);
                print(opToken);
                if (!compile(child)) {
                    return false;
                }
                break;
            }
            case postop: {
                Token opToken = node.getASTChild(ASTNodeType.operator).getToken();
                TypeAugmentedSemanticNode child = node.getChild(0);
                if (!compile(child)) {
                    return false;
                }
                print(opToken);
                break;
            }
            case binop: {
                Token opToken = node.getASTChild(ASTNodeType.operator).getToken();
                TypeAugmentedSemanticNode lhs = node.getChild(1);
                TypeAugmentedSemanticNode rhs = node.getChild(2);
                print("(");
                if (!compile(lhs)) {
                    return false;
                }
                //print(" ");
                print(opToken);
                //print(" ");
                if (!compile(rhs)) {
                    return false;
                }
                print(")");
                break;
            }
            case cast: {
                CXType castType = node.getCXType();
                if(castType == null && node.getASTNode() instanceof TypedAbstractSyntaxNode) {
                    castType = ((TypedAbstractSyntaxNode) node.getASTNode()).getCxType();
                }
                TypeAugmentedSemanticNode child = node.getChild(0);
                print("(");
                print(castType.generateCDefinition());
                print(") ");
                if(!compile(child)) return false;
                break;
            }
            case addressof: {
                print("&");
                print("(");
                TypeAugmentedSemanticNode child = node.getChild(0);
                if(!compile(child)) return false;
                print(')');
                break;
            }
            case indirection: {
                print('(');
                print("*");
                TypeAugmentedSemanticNode child = node.getChild(0);
                if(!compile(child)) return false;
                print(')');
                break;
            }
            case array_reference: {
                TypeAugmentedSemanticNode lhs = node.getChild(0);
                TypeAugmentedSemanticNode rhs = node.getChild(1);
                
                if(!compile(lhs)) return false;
                print("[");
                if(!compile(rhs)) return false;
                print("]");
                break;
            }
            case function_call: {
                TypeAugmentedSemanticNode call = node.getChild(0);
                TypeAugmentedSemanticNode sequence = node.getASTChild(ASTNodeType.sequence);
                
                if(!compile(call)) return false;
                print("(");
                if(!compile(sequence)) return false;
                print(")");
                break;
            }
            case sequence: {
                boolean first = true;
                for (TypeAugmentedSemanticNode child : node.getChildren()) {
                    if(first) first = false;
                    else print(", ");
                    if(!compile(child)) return false;
                }
                
                break;
            }
            case ternary: {
                TypeAugmentedSemanticNode expression = node.getChild(0);
                TypeAugmentedSemanticNode lhs = node.getChild(1);
                TypeAugmentedSemanticNode rhs = node.getChild(2);
                
                print("(");
                if(!compile(expression)) return false;
                print("?");
                if(!compile(lhs)) return false;
                print(":");
                if(!compile(rhs)) return false;
                print(")");
                break;
            }
            case method_call: {
                TypeAugmentedSemanticNode caller = node.getChild(0);
                boolean needToGetReference = caller.getASTType() == ASTNodeType.id;
                if(UniversalCompilerSettings.getInstance().getSettings().isReduceIndirection() && node.getChild(0).getASTType() == ASTNodeType.indirection) {
                    caller = caller.getChild(0);
                }
                
                
                String objectInteractionImage;
                boolean isLValueMethodCall = caller.isLValue();
                
                
                String sequence = compileToString(node.getASTChild(ASTNodeType.sequence));
                if(sequence == null) return false;
                int sequenceLength = sequence.isBlank()? 0 : sequence.split(",").length;
                
                if(node.containsCompilationTag(SuperCallTag.class)) {
                    SuperCallTag compilationTag = node.getCompilationTag(SuperCallTag.class);
                    if(!sequence.isBlank())
                        print(compilationTag.getMethod().methodAsFunctionCall("super", sequence));
                    else
                        print(compilationTag.getMethod().methodAsFunctionCall("super"));
                    
                    return true;
                }
                
                MethodCallTag methodCallTag = node.getCompilationTag(MethodCallTag.class);
                
                
                objectInteractionImage = compileToString(caller); // save for later use
                if (objectInteractionImage == null) return false;
                
                String callingOnString;
                if(node.getChild(0).containsCompilationTag(BasicCompilationTag.NEW_OBJECT_DEREFERENCE)) {
                    callingOnString = objectInteractionImage;
                } else {
                    
                    if(isLValueMethodCall) {
                        print(objectInteractionImage);
                        
                    }
                    if(!caller.containsCompilationTag(BasicCompilationTag.INDIRECT_FIELD_GET)) {
                        if (!needToGetReference) {
                            CXType firstType = methodCallTag.getMethod().getParent().toPointer();
                            TypeAugmentedSemanticNode ptr = caller;
                            TypeEnvironment environment = methodCallTag.getMethod().getParent().getEnvironment();
                            while (!ptr.getCXType().is(firstType, environment)) {
                                ptr = ptr.getChild(0);
                            }
                            
                            if (ptr.getCXType() instanceof ArrayType) {
                                CXType type = ptr.getCXType();
                                while (type instanceof ArrayType) {
                                    ptr = ptr.getParent();
                                    type = ptr.getCXType();
                                }
                            }
                            
                            
                            callingOnString = compileToString(ptr); // save for later use
                            if (callingOnString == null) return false;
                            if (!(ptr.getCXType() instanceof PointerType))
                                callingOnString = "&" + callingOnString;
                            
                            CXType type = ptr.getCXType();
                            while (type instanceof PointerType && ((PointerType) type).getSubType() instanceof PointerType) {
                                callingOnString = "*" + callingOnString;
                                type = ((PointerType) type).getSubType();
                                
                            }
                        } else {
                            callingOnString = "&" + objectInteractionImage;
                        }
                    } else {
                        callingOnString = objectInteractionImage;
                    }
                    
                    if(!isLValueMethodCall) {
                        
                        CXType firstType = methodCallTag.getMethod().getParent().toPointer();
                        println("({");
                        println("\t" + firstType.generateCDeclaration("__temp") +  " = " + callingOnString + ";");
                        print("\t(*__temp)");
                        callingOnString = "__temp";
                    }
                    
                }
                
                if(!isLValueMethodCall && !node.getChild(0).containsCompilationTag(BasicCompilationTag.NEW_OBJECT_DEREFERENCE)) {
                    
                    
                    boolean isVirtualCall =
                            node.containsCompilationTag(BasicCompilationTag.VIRTUAL_METHOD_CALL);
                    if(!UniversalCompilerSettings.getInstance().getSettings().isReduceIndirection() || !(caller.getCXType() instanceof PointerType))
                        print('.');
                    else
                        print("->");
                    
                    if (isVirtualCall) {
                        print(UniversalCompilerSettings.getInstance().getSettings().getvTableName());
                        print("->");
                    }
                    
                    
                    if (sequence.isEmpty()) {
                        print(methodCallTag.getMethod().methodCall(callingOnString));
                    } else {
                        print(methodCallTag.getMethod().methodCall(callingOnString, sequence));
                    }
                    
                    println(";");
                    print("})");
                } else if(node.getChild(0).containsCompilationTag(BasicCompilationTag.NEW_OBJECT_DEREFERENCE)){
                    /*
                    TypeAugmentedSemanticNode original = node.getChild(0).getChild(0); // get constructor call
                    objectInteractionImage = compileToString(original);
                    if(objectInteractionImage == null) return false;
                    print(objectInteractionImage);
                    */
                    if(sequence == null) return false;
                    if (sequence.isEmpty()) {
                        print(methodCallTag.getMethod().methodAsFunctionCall(callingOnString));
                    } else {
                        print(methodCallTag.getMethod().methodAsFunctionCall(callingOnString, sequence));
                    }
                    
                } else if(caller.containsCompilationTag(BasicCompilationTag.INDIRECT_FIELD_GET)) {
                    
                    // TODO FIGURE THIS OUT
                    
                    if(!(caller.getCXType() instanceof CXFunctionPointer)) return true;
                    int size = ((CXFunctionPointer) caller.getCXType()).getParameterTypeList().getSize();
                    
                    
                    print("(");
                    if(sequenceLength == size - 1) {
                        print(callingOnString);
                        if (!sequence.isEmpty()) {
                            print(", ");
                            print(sequence);
                        }
                    } else {
                        print(sequence);
                    }
                    print(")");
                }else {
                    boolean isVirtualCall =
                            node.containsCompilationTag(BasicCompilationTag.VIRTUAL_METHOD_CALL);
                    if(!UniversalCompilerSettings.getInstance().getSettings().isReduceIndirection())
                        print('.');
                    else
                        print("->");
                    
                    if (isVirtualCall) {
                        
                        print(UniversalCompilerSettings.getInstance().getSettings().getvTableName());
                        print("->");
                    }
                    
                    
                    if(sequence == null) return false;
                    if (sequence.isEmpty()) {
                        print(methodCallTag.getMethod().methodCall(callingOnString));
                    } else {
                        print(methodCallTag.getMethod().methodCall(callingOnString, sequence));
                    }
                }
                
                
                
                break;
            }
            case field_get: {
                TypeAugmentedSemanticNode objectInteraction = node.getChild(0);
                boolean isIndirect = objectInteraction.containsCompilationTag(BasicCompilationTag.INDIRECT_FIELD_GET);
                
                if(!isIndirect && UniversalCompilerSettings.getInstance().getSettings().isReduceIndirection()) {
                    if(!compile(objectInteraction.getChild(0))) return false;
                    print("->");
                } else if (!isIndirect) {
                    if(!compile(objectInteraction)) return false;
                    print('.');
                } else {
                    if(!compile(objectInteraction.getChild(0))) return false;
                    print("->");
                }
                
                if(!compile(node.getChild(1))) return false;
                
                break;
            }
            case constructor_call: {
                ConstructorCallTag compilationTag = node.getCompilationTag(ConstructorCallTag.class);
                if(compilationTag == null) return false;
                TypeAugmentedSemanticNode sequence = node.getASTChild(ASTNodeType.sequence);
                String sequenceString = compileToString(sequence);
                if(sequenceString == null) return false;
                
                CXClassType classType = compilationTag.getConstructor().getParent();
                
                CXMethod initMethod = classType.getInitMethod();
                String initCall = initMethod.methodAsFunctionCall("");
                String fullCall;
                if(sequenceString.isEmpty()) {
                    fullCall = compilationTag.getConstructor().methodAsFunctionCall(initCall);
                } else {
                    fullCall = compilationTag.getConstructor().methodAsFunctionCall(initCall, sequenceString);
                }
                print(fullCall);
            }
            case empty: {
                break;
            }
            default:
                
                return false;
        }
        
        
        return true;
    }
    
    
}
