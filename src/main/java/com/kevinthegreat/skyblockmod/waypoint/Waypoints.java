package com.kevinthegreat.skyblockmod.waypoint;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kevinthegreat.skyblockmod.SkyblockMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

public class Waypoints {
    public static final Logger LOGGER = LoggerFactory.getLogger(Waypoints.class);
    private static final Codec<List<WaypointCategory>> CODEC = WaypointCategory.CODEC.listOf();
    private static final Codec<List<WaypointCategory>> SKYTILS_CODEC = WaypointCategory.SKYTILS_CODEC.listOf();
    private static final Path waypointsFile = FabricLoader.getInstance().getConfigDir().resolve(SkyblockMod.MOD_ID).resolve("waypoints.json");
    static final Multimap<String, WaypointCategory> waypoints = MultimapBuilder.hashKeys().arrayListValues().build();

    public static void init() {
        loadWaypoints();
        ClientLifecycleEvents.CLIENT_STOPPING.register(Waypoints::saveWaypoints);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(Waypoints::render);
    }

    public static void loadWaypoints() {
        waypoints.clear();
        try (BufferedReader reader = Files.newBufferedReader(waypointsFile)) {
            List<WaypointCategory> waypoints = CODEC.parse(JsonOps.INSTANCE, SkyblockMod.GSON.fromJson(reader, JsonArray.class)).resultOrPartial(LOGGER::error).orElseThrow();
            waypoints.forEach(waypointCategory -> Waypoints.waypoints.put(waypointCategory.island(), waypointCategory));
        } catch (Exception e) {
            LOGGER.error("[Skyblocker Waypoints] Encountered exception while loading waypoints", e);
        }
    }

    public static List<WaypointCategory> fromSkytilsBase64(String base64) {
        return fromSkytilsJson(new String(Base64.getDecoder().decode(base64)));
    }

    public static List<WaypointCategory> fromSkytilsJson(String waypointCategories) {
        return SKYTILS_CODEC.parse(JsonOps.INSTANCE, SkyblockMod.GSON.fromJson(waypointCategories, JsonObject.class).getAsJsonArray("categories")).resultOrPartial(LOGGER::error).orElseThrow();
    }

    public static String toSkytilsBase64(Collection<WaypointCategory> waypointCategories) {
        return Base64.getEncoder().encodeToString(toSkytilsJson(waypointCategories).getBytes());
    }

    public static String toSkytilsJson(Collection<WaypointCategory> waypointCategories) {
        JsonObject waypointCategoriesJson = new JsonObject();
        waypointCategoriesJson.add("categories", SKYTILS_CODEC.encodeStart(JsonOps.INSTANCE, List.copyOf(waypointCategories)).resultOrPartial(LOGGER::error).orElseThrow());
        return SkyblockMod.GSON.toJson(waypointCategoriesJson);
    }

    public static void saveWaypoints(MinecraftClient client) {
        try (BufferedWriter writer = Files.newBufferedWriter(waypointsFile)) {
            JsonElement waypointsJson = CODEC.encodeStart(JsonOps.INSTANCE, List.copyOf(waypoints.values())).resultOrPartial(LOGGER::error).orElseThrow();
            SkyblockMod.GSON.toJson(waypointsJson, writer);
            LOGGER.info("[Skyblocker Waypoints] Saved waypoints");
        } catch (Exception e) {
            LOGGER.error("[Skyblocker Waypoints] Encountered exception while saving waypoints", e);
        }
    }

    public static void render(WorldRenderContext context) {
        Collection<WaypointCategory> categories = waypoints.get(SkyblockMod.skyblockMod.info.locationRaw);
        for (WaypointCategory category : categories) {
            if (category != null) {
                category.render(context);
            }
        }
    }
}
