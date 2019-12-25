package radin.interphase.semantics.types;

import radin.interphase.semantics.TypeEnvironment;
import radin.interphase.semantics.exceptions.TypeDoesNotExist;
import radin.interphase.semantics.types.compound.CXCompoundType;

public class CompoundTypeReference extends CXType {
    
    public enum CompoundType {
        struct("struct"),
        union("struct"),
        _class("class")
        ;
        String cequiv;
        
        CompoundType(String cequiv) {
            this.cequiv = cequiv;
        }
    }
    private CompoundType compoundType;
    private String typename;
    
    public CompoundTypeReference(CompoundType compoundType, String typename) {
        this.compoundType = compoundType;
        this.typename = typename;
    }
    
    public CompoundTypeReference(CompoundType compoundType, CXCompoundType actual) {
        this.compoundType = compoundType;
        this.typename = actual.getTypeName();
    }
    
    @Override
    public String generateCDefinition() {
        return compoundType.cequiv + " " + typename;
    }
    
    @Override
    public String generateCDefinition(String identifier) {
        return compoundType.cequiv + " " + typename + " " + identifier;
    }
    
    @Override
    public boolean is(CXType other, TypeEnvironment e) {
        if(!(other instanceof ICXCompoundType || other instanceof CompoundTypeReference)) return false;
        if(other instanceof ICXCompoundType) {
            return e.getNamedCompoundType(typename).is(other, e);
        } else {
            if(!e.namedCompoundTypeExists(((CompoundTypeReference) other).typename)) throw new TypeDoesNotExist(((CompoundTypeReference) other).typename);
            return e.getNamedCompoundType(typename).is(e.getNamedCompoundType(((CompoundTypeReference) other).typename), e);
        }
    }
    
    @Override
    public boolean isValid(TypeEnvironment e) {
        return e.namedCompoundTypeExists(typename);
    }
    
    @Override
    public boolean isPrimitive() {
        return false;
    }
    
    @Override
    public long getDataSize(TypeEnvironment e) {
        return e.getNamedCompoundType(typename).getDataSize(e);
    }
}
