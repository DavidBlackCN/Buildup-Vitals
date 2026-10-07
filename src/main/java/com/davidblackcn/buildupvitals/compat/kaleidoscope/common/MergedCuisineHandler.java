package com.davidblackcn.buildupvitals.compat.kaleidoscope.common;
import org.spongepowered.asm.mixin.injection.selectors.*;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.Annotations;

/** Metadata-only, exact upstream mixin + handler selection; no generated hash assumptions or bytecode edits. */
@ITargetSelectorDynamic.SelectorId("merged")
public final class MergedCuisineHandler implements ITargetSelectorDynamic {
    private final String mixin, handler;
    private MergedCuisineHandler(String mixin,String handler){this.mixin=mixin;this.handler=handler;}
    public static MergedCuisineHandler parse(String input,ISelectorContext context) {
        String[] parts=input.split("#",-1);
        if(parts.length!=2 || parts[0].isBlank() || parts[1].isBlank())throw new InvalidSelectorException("Expected exact mixin#handler");
        return new MergedCuisineHandler(parts[0],parts[1]);
    }
    @Override public ITargetSelector next(){return null;}
    @Override public ITargetSelector configure(Configure request,String... args){return this;}
    @Override public ITargetSelector validate(){return this;}
    @Override public ITargetSelector attach(ISelectorContext context){return this;}
    @Override public int getMinMatchCount(){return 1;}
    @Override public int getMaxMatchCount(){return 1;}
    @Override public <T> MatchResult match(ElementNode<T> node){
        if(node==null || node.getMethod()==null || !node.getName().endsWith("$"+handler))return MatchResult.NONE;
        var annotation=Annotations.getVisible(node.getMethod(),MixinMerged.class);
        return annotation!=null && mixin.equals(Annotations.<String>getValue(annotation,"mixin")) ? MatchResult.EXACT_MATCH:MatchResult.NONE;
    }
    @Override public String toString(){return mixin+"#"+handler;}
}
