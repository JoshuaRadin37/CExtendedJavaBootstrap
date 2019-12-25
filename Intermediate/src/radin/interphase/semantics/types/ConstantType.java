package radin.interphase.semantics.types;

import radin.interphase.semantics.TypeEnvironment;

import javax.print.attribute.standard.Fidelity;

public class ConstantType extends CXType {
    
    private CXType subtype;
    
    public ConstantType(CXType subtype) {
        this.subtype = subtype;
    }
    
    @Override
    public String generateCDefinition() {
        return "const " + subtype.generateCDefinition();
    }
    
    @Override
    public String generateCDefinition(String identifier) {
        return "const " + subtype.generateCDefinition(identifier);
    }
    
    @Override
    public boolean isValid(TypeEnvironment e) {
        return subtype.isValid(e);
    }
    
    @Override
    public boolean isPrimitive() {
        return subtype.isPrimitive();
    }
    
    @Override
    public long getDataSize(TypeEnvironment e) {
        return subtype.getDataSize(e);
    }
    
    @Override
    public boolean is(CXType other, TypeEnvironment e) {
        return subtype.is(other, e);
    }
}
