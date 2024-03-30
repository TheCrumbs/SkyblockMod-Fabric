package com.kevinthegreat.skyblockmod.waypoint;

import com.google.common.primitives.Floats;
import com.kevinthegreat.skyblockmod.SkyblockMod;
import com.kevinthegreat.skyblockmod.util.RenderHelper;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class NamedWaypoint extends Waypoint {
    public static final Codec<NamedWaypoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(secretWaypoint -> secretWaypoint.pos),
            TextCodecs.CODEC.fieldOf("name").forGetter(secretWaypoint -> secretWaypoint.name),
            Codec.floatRange(0, 1).listOf().comapFlatMap(
                    colorComponentsList -> colorComponentsList.size() == 3 ? DataResult.success(Floats.toArray(colorComponentsList)) : DataResult.error(() -> "Expected 3 color components, got " + colorComponentsList.size() + " instead"),
                    Floats::asList
            ).fieldOf("colorComponents").forGetter(secretWaypoint -> secretWaypoint.colorComponents),
            Codec.BOOL.fieldOf("shouldRender").forGetter(Waypoint::shouldRender)
    ).apply(instance, NamedWaypoint::new));
    public static final Codec<NamedWaypoint> SKYTILS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(waypoint -> waypoint.pos.getX()),
            Codec.INT.fieldOf("y").forGetter(waypoint -> waypoint.pos.getY()),
            Codec.INT.fieldOf("z").forGetter(waypoint -> waypoint.pos.getZ()),
            Codec.either(Codec.STRING, Codec.INT).xmap(either -> either.map(str -> str, Object::toString), Either::left).fieldOf("name").forGetter(waypoint -> waypoint.name.getString()),
            Codec.INT.fieldOf("color").forGetter(waypoint -> (int) (waypoint.colorComponents[0] * 255) << 16 | (int) (waypoint.colorComponents[1] * 255) << 8 | (int) (waypoint.colorComponents[2] * 255)),
            Codec.BOOL.fieldOf("enabled").forGetter(Waypoint::shouldRender)
    ).apply(instance, NamedWaypoint::fromSkytils));
    protected final Text name;
    protected final Vec3d centerPos;

    public NamedWaypoint(BlockPos pos, String name, float[] colorComponents) {
        this(pos, name, colorComponents, true);
    }

    public NamedWaypoint(BlockPos pos, String name, float[] colorComponents, boolean shouldRender) {
        this(pos, Text.of(name), colorComponents, shouldRender);
    }

    public NamedWaypoint(BlockPos pos, Text name, float[] colorComponents, boolean shouldRender) {
        this(pos, name, () -> Optional.ofNullable(SkyblockMod.skyblockMod).map(skyblockMod -> skyblockMod.options.waypointType.getValue()).orElse(Waypoint.Type.WAYPOINT), colorComponents, shouldRender);
    }

    public NamedWaypoint(BlockPos pos, String name, Supplier<Type> typeSupplier, float[] colorComponents, boolean shouldRender) {
        this(pos, Text.of(name), typeSupplier, colorComponents, shouldRender);
    }

    public NamedWaypoint(BlockPos pos, Text name, Supplier<Type> typeSupplier, float[] colorComponents) {
        this(pos, name, typeSupplier, colorComponents, true);
    }

    public NamedWaypoint(BlockPos pos, Text name, Supplier<Type> typeSupplier, float[] colorComponents, boolean shouldRender) {
        super(pos, typeSupplier, colorComponents, DEFAULT_HIGHLIGHT_ALPHA, DEFAULT_LINE_WIDTH, true, shouldRender);
        this.name = name;
        this.centerPos = pos.toCenterPos();
    }

    public static NamedWaypoint fromSkytils(int x, int y, int z, String name, int color, boolean enabled) {
        return new NamedWaypoint(new BlockPos(x, y, z), name, new float[]{((color & 0x00FF0000) >> 16) / 255f, ((color & 0x0000FF00) >> 8) / 255f, (color & 0x000000FF) / 255f}, enabled);
    }

    public NamedWaypoint copy() {
        return new NamedWaypoint(pos, name, typeSupplier, colorComponents, shouldRender());
    }

    @Override
    public NamedWaypoint withX(int x) {
        return new NamedWaypoint(new BlockPos(x, pos.getY(), pos.getZ()), name, typeSupplier, getColorComponents(), shouldRender());
    }

    @Override
    public NamedWaypoint withY(int y) {
        return new NamedWaypoint(pos.withY(y), name, typeSupplier, getColorComponents(), shouldRender());
    }

    @Override
    public NamedWaypoint withZ(int z) {
        return new NamedWaypoint(new BlockPos(pos.getX(), pos.getY(), z), name, typeSupplier, getColorComponents(), shouldRender());
    }

    @Override
    public NamedWaypoint withColor(float[] colorComponents) {
        return new NamedWaypoint(pos, name, typeSupplier, colorComponents, shouldRender());
    }

    public Text getName() {
        return name;
    }

    public NamedWaypoint withName(String name) {
        return new NamedWaypoint(pos, name, typeSupplier, getColorComponents(), shouldRender());
    }

    protected boolean shouldRenderName() {
        return true;
    }

    @Override
    public void render(WorldRenderContext context) {
        super.render(context);
        if (shouldRenderName()) {
            RenderHelper.renderText(context, name, centerPos.add(0, 1, 0), true);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || super.equals(obj) && obj instanceof NamedWaypoint waypoint && name.equals(waypoint.name);
    }
}
