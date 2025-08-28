package xyz.crunchmunch.mods.gamemaster.mixin.devtools;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(ClientboundResourcePackPushPacket.class)
public abstract class ClientboundResourcePackPushPacketMixin {
    @Shadow @Final @Mutable
    private Optional<Component> prompt;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void devtools$fixNullPrompt(UUID id, String url, String hash, boolean required, Optional<Component> prompt, CallbackInfo ci) {
        // ??
        this.prompt = prompt == null ? Optional.empty() : prompt;
    }
}
