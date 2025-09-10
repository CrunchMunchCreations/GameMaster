package xyz.crunchmunch.mods.gamemaster.mixin.accessors;

import com.mojang.math.Transformation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {
    @Accessor("DATA_TRANSLATION_ID")
    static EntityDataAccessor<Vector3f> getDataTranslationId() {
        throw new UnsupportedOperationException();
    }

    @Accessor("DATA_SCALE_ID")
    static EntityDataAccessor<Vector3f> getDataScaleId() {
        throw new UnsupportedOperationException();
    }

    @Accessor("DATA_LEFT_ROTATION_ID")
    static EntityDataAccessor<Quaternionf> getDataLeftRotationId() {
        throw new UnsupportedOperationException();
    }

    @Accessor("DATA_RIGHT_ROTATION_ID")
    static EntityDataAccessor<Quaternionf> getDataRightRotationId() {
        throw new UnsupportedOperationException();
    }

    @Invoker
    void callSetViewRange(float viewRange);

    @Invoker
    float callGetViewRange();

    @Invoker
    int callGetPosRotInterpolationDuration();

    @Invoker
    void callSetPosRotInterpolationDuration(int posRotInterpolationDuration);

    @Invoker
    void callSetTransformationInterpolationDelay(int transformationInterpolationDelay);

    @Invoker
    int callGetTransformationInterpolationDelay();

    @Invoker
    void callSetShadowRadius(float shadowRadius);

    @Invoker
    float callGetShadowRadius();

    @Invoker
    void callSetShadowStrength(float shadowStrength);

    @Invoker
    float callGetShadowStrength();

    @Invoker
    void callSetWidth(float width);

    @Invoker
    float callGetWidth();

    @Invoker
    void callSetHeight(float height);

    @Invoker
    float callGetHeight();

    @Invoker
    void callSetBrightnessOverride(@Nullable Brightness brightnessOverride);

    @Invoker
    void callSetBillboardConstraints(Display.BillboardConstraints billboardConstraints);

    @Invoker
    Display.BillboardConstraints callGetBillboardConstraints();

    @Invoker
    void callSetTransformation(Transformation transformation);

    @Invoker
    int callGetTransformationInterpolationDuration();

    @Invoker
    void callSetTransformationInterpolationDuration(int transformationInterpolationDuration);

    @Invoker
    static Transformation invokeCreateTransformation(SynchedEntityData data) {
        throw new IllegalStateException();
    }
}

