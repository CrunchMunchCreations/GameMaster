package xyz.crunchmunch.mods.gamemaster.mixin.devtools;

import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Path.class)
public abstract class PathMixin {
//    @Shadow @Nullable private Path.DebugData debugData;
//
//    @Shadow @Final private List<Node> nodes;
//
//    @Shadow @Final private BlockPos target;
//
//    @Inject(method = "writeToStream", at = @At("HEAD"))
//    private void devtools$writeToStream(FriendlyByteBuf buffer, CallbackInfo ci) {
//        this.debugData = new Path.DebugData(
//            this.nodes.stream().filter(node -> !node.closed).toArray(Node[]::new),
//            this.nodes.stream().filter(node -> node.closed).toArray(Node[]::new),
//            Set.of(new Target(this.target.getX(), this.target.getY(), this.target.getZ()))
//        );
//    }
}
