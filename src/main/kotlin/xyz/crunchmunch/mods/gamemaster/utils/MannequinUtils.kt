package xyz.crunchmunch.mods.gamemaster.utils

import com.mojang.authlib.GameProfile
import com.mojang.datafixers.util.Either
import net.minecraft.core.ClientAsset
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.decoration.Mannequin
import net.minecraft.world.entity.player.PlayerModelType
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.item.component.ResolvableProfile
import java.util.*

fun Mannequin.doesSkinMatch(texture: ResourceLocation): Boolean {
    return this.profile.skinPatch().body.isPresent && this.profile.skinPatch().body.get().id == texture
}

fun Mannequin.applySkin(profile: GameProfile) {
    this.profile = ResolvableProfile.createResolved(profile)
}

fun Mannequin.applySkin(texture: ResourceLocation, type: PlayerModelType) {
    this.profile = ResolvableProfile.create(Either.left(this.profile.partialProfile()),
        PlayerSkin.Patch.create(
            Optional.of(ClientAsset.ResourceTexture(texture)),
            Optional.empty(),
            Optional.empty(),
            Optional.of(type)
        ))
}