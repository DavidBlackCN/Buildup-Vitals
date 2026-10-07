package com.davidblackcn.buildupvitals.compat;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.MergedCuisineHandler;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.selectors.*;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.Annotations;
import static org.junit.jupiter.api.Assertions.*;
class MergedCuisineHandlerTest {
    @Test void selectsExactOwnerWithoutGeneratedHashOrGameClasses() {
        var selector=MergedCuisineHandler.parse("upstream.ExactMixin#onAddEffect",null);
        var owner=new ClassNode();owner.name="net/minecraft/world/entity/LivingEntity";
        var method=new MethodNode();method.name="handler$random$mod$onAddEffect";method.desc="()V";
        Annotations.setVisible(method,MixinMerged.class,"mixin","upstream.ExactMixin");
        assertEquals(MatchResult.EXACT_MATCH,selector.match(ElementNode.of(owner,method)));
        Annotations.setVisible(method,MixinMerged.class,"mixin","unrelated.ExactMixin");
        assertEquals(MatchResult.NONE,selector.match(ElementNode.of(owner,method)));
        assertEquals(1,selector.getMaxMatchCount());assertEquals(1,selector.getMinMatchCount());
        assertThrows(InvalidSelectorException.class,()->MergedCuisineHandler.parse("missing-owner",null));
    }
}
