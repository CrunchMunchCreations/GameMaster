package xyz.crunchmunch.mods.gamemaster.mixin.dialogs;

import com.mojang.serialization.Codec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.crunchmunch.mods.gamemaster.server.CustomClickHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {
    // We don't inject in MinecraftServer#handleCustomClickAction, we don't have access to the player there.
    @Inject(method = "handleCustomClickAction", at = @At("TAIL"), cancellable = true)
    private void chrunchy$handleRegisteredCustomClicks(ServerboundCustomClickActionPacket packet, CallbackInfo ci) {
        Codec<? extends CustomClickHandler> handlerCodec = CustomClickHandler.REGISTRY.getValue(packet.id());

        if (handlerCodec != null && (Object) this instanceof ServerGamePacketListenerImpl gamePacketListener) {
            CustomClickHandler handler = handlerCodec.decode(NbtOps.INSTANCE, packet.payload().orElseGet(CompoundTag::new))
                .getOrThrow().getFirst();

            handler.handle(gamePacketListener.getPlayer());
            ci.cancel();
        }
    }
}
