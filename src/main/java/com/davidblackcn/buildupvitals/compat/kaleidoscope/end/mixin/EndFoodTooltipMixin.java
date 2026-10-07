package com.davidblackcn.buildupvitals.compat.kaleidoscope.end.mixin;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.end.EndEffects;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets={"com.github.ysbbbbbb.kaleidoscopecookery.item.FoodWithEffectsItem","com.bmt.kaleidoscope_end.item.KEBowlFoodBlockItem"})
public abstract class EndFoodTooltipMixin {
    @WrapOperation(method="appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/alchemy/PotionContents;addPotionTooltip(Ljava/lang/Iterable;Ljava/util/function/Consumer;FF)V"))
    private void buildupVitals$reviewTooltip(Iterable<MobEffectInstance> effects,Consumer<Component> output,float scale,float rate,Operation<Void> original,ItemStack stack,Item.TooltipContext context,TooltipDisplay display,Consumer<Component> consumer,TooltipFlag flag){
        var reviewed=new ArrayList<MobEffectInstance>();for(var effect:effects){var next=EndEffects.review(stack,effect);if(next!=null)reviewed.add(next);}original.call(reviewed,output,scale,rate);
    }
}
