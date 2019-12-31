package radin.interphase.semantics.types.compound;

import radin.interphase.semantics.TypeEnvironment;
import radin.interphase.semantics.types.CXType;
import radin.interphase.semantics.types.methods.ParameterTypeList;
import radin.interphase.semantics.types.primitives.AbstractCXPrimitiveType;
import radin.interphase.semantics.types.primitives.CXPrimitiveType;

import java.util.Arrays;
import java.util.List;

public class CXFunctionPointer extends AbstractCXPrimitiveType {
    
    private CXType returnType;
    private List<CXType> parameterTypes;
    
    
    public CXFunctionPointer(CXType returnType, List<CXType> parameterTypes) {
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }
    
    public CXFunctionPointer(CXType returnType) {
        this.returnType = returnType;
        parameterTypes = Arrays.asList(CXPrimitiveType.VOID);
    }
    
    public List<CXType> getParameterTypes() {
        return parameterTypes;
    }
    
    @Override
    public boolean isValid(TypeEnvironment e) {
        if(!returnType.isValid(e)) return false;
        for (CXType parameterType : parameterTypes) {
            if(!parameterType.isValid(e)) return false;
        }
        return true;
    }
    
    @Override
    public boolean isIntegral() {
        return false;
    }
    
    public ParameterTypeList getParameterTypeList() {
        return new ParameterTypeList(parameterTypes);
    }
    
    @Override
    public long getDataSize(TypeEnvironment e) {
        return e.getPointerSize();
    }
    
    @Override
    public String generateCDefinition() {
        StringBuilder parameters = new StringBuilder();
        boolean first = true;
        for (CXType parameterType : parameterTypes) {
            if(first) first = false;
            else parameters.append(", ");
            
            parameters.append(parameterType.generateCDefinition());
        }
        return returnType.generateCDefinition() + " (*) (" + parameters.toString() + ")";
    }
    
    public CXType getReturnType() {
        return returnType;
    }
    
    @Override
    public boolean is(CXType other, TypeEnvironment e, boolean strictPrimitiveEquality) {
        if(!(other instanceof CXFunctionPointer)) return false;
        CXFunctionPointer that = (CXFunctionPointer) other;
        
        if(!that.returnType.is(returnType, e)) return false;
        
        return that.getParameterTypeList().equals(this.getParameterTypeList(), e);
    }
    
    @Override
    public String generateCDefinition(String identifier) {
        StringBuilder parameters = new StringBuilder();
        boolean first = true;
        for (CXType parameterType : parameterTypes) {
            if(first) first = false;
            else parameters.append(", ");
        
            parameters.append(parameterType.generateCDefinition());
        }
        return returnType.generateCDefinition() + " (*" + identifier + ") (" + parameters.toString() + ")";
    }
}