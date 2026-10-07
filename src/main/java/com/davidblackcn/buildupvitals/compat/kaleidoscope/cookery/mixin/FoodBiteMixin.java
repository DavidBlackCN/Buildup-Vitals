package com.davidblackcn.buildupvitals.compat.kaleidoscope.cookery.mixin;

import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineConsumption;
import com.davidblackcn.buildupvitals.food.overeating.PlayerOvereat;
import com.davidblackcn.buildupvitals.food.recovery.PlayerRecovery;
import com.davidblackcn.buildupvitals.hydration.HydrationAdapter;
import com.davidblackcn.buildupvitals.compat.thirst.ThirstBridge;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock")
public abstract class FoodBiteMixin {
    @Shadow @Final protected int maxBites;
    @Shadow @Final protected IntegerProperty bites;
    @Shadow @Final protected Consumable consumable;

    @Inject(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"))
    private void buildupVitals$remainingMainEffect(Level level, BlockPos pos, BlockState state, Player user,
                                                   CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        if (!(user instanceof ServerPlayer player) || !cir.getReturnValue().consumesAction()) return;
        var stack = ((Block) (Object) this).asItem().getDefaultInstance();
        // Cookery's bite path applies only effects.getFirst(). Bridge a profile's main effect
        // from later entries too. End also needs its reviewed secondary mythic slot applied.
        for (var action : consumable.onConsumeEffects()) {
            if (action instanceof net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect applied) {
                for (var raw : applied.effects().stream().skip(1).toList()) {
                    var effect = com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectAdapter.review(stack, raw);
                    if (effect == null) continue;
                    var id = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
                    if ((com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectAdapter.isForeignCuisineEffect(id)
                            && com.davidblackcn.buildupvitals.food.benefit.ForeignMealBenefits.registered(id)
                            || com.davidblackcn.buildupvitals.compat.kaleidoscope.common.KaleidoscopeVersion.supported("end") && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("kaleidoscope_end"))
                            && CuisineConsumption.allowEffect(player, stack, effect) && level.getRandom().nextFloat() < applied.probability()) {
                        player.addEffect(new MobEffectInstance(effect));
                    }
                }
            }
        }
    }

    @WrapOperation(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    private void buildupVitals$portion(FoodData data, int nutrition, float saturation, Operation<Void> original,
                                       Level level, BlockPos pos, BlockState state, Player user) {
        if (user instanceof ServerPlayer player) PlayerOvereat.foodConsumed(player, nutrition);
        original.call(data, nutrition, saturation);
        if (user instanceof ServerPlayer player) {
            ItemStack stack = ((Block) (Object) this).asItem().getDefaultInstance();
            stack.set(DataComponents.CONSUMABLE, consumable);
            PlayerRecovery.foodConsumed(player, stack, 1.0 / maxBites);
            if (HydrationAdapter.enabled()) ThirstBridge.drinkPortion(player, stack, state.getValue(bites), maxBites);
        }
    }

    @WrapOperation(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean buildupVitals$mainBenefit(Player user, MobEffectInstance effect, Operation<Boolean> original,
                                             Level level, BlockPos pos, BlockState state, Player player) {
        effect = com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectAdapter.review(((Block) (Object) this).asItem().getDefaultInstance(), effect);
        if (effect == null) return false;
        if (user instanceof ServerPlayer server && !CuisineConsumption.allowEffect(server,
                ((Block) (Object) this).asItem().getDefaultInstance(), effect)) return false;
        return original.call(user, effect);
    }
}
