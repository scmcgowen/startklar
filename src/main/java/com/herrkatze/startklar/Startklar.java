package com.herrkatze.startklar;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class Startklar implements ModInitializer {
	public static final String MOD_ID = "startklar";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String BYPASS_PERMISSION = "startklar.bypass_double_tap_elytra";

    public static final Config CONFIG = Config.createToml(FabricLoader.getInstance().getConfigDir(), MOD_ID,
            "server", Config.class);

    /**
     * 0 = NO FLYING
     * 1 = ALLOW FLYING
     * 2 = ALLOW FLYING WITH BOOST
     */
    public static final AttachmentType<Integer> FLYING_SINCE_SPAWN_TYPE = AttachmentRegistry.createDefaulted(
            Startklar.of("player_extended_data"),
            () -> 0
    );


    @Override
    public void onInitialize() {
        // Elytra start event
        ServerTickEvents.END_WORLD_TICK.register(Startklar::startFlying);

        // Elytra - Allow keep flying
        EntityElytraEvents.CUSTOM.register(Startklar::keepFlying);

        // Flying boost & status indicator
        if (CONFIG.flightDuration > 0) {
            ItemStack flightDuration = Items.FIREWORK_ROCKET.getDefaultInstance();
            CompoundTag flightTag = new CompoundTag();
            flightTag.putByte("Flight", (byte) CONFIG.flightDuration);

            ServerTickEvents.END_WORLD_TICK.register((level) -> flyingBoost(level, flightDuration));
        }

        // Cancel fall damage after flying
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(Startklar::cancelDamageUponLanding);

        LOGGER.info("Wir sind startklar!");
    }

    /**
     * Allows players near spawn to start flying upon call.
     */
    private static void startFlying(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD || level.isClientSide())
            return;

        int diameter = CONFIG.spawnDiameter;
        Vec3 posSpawn = level.getSharedSpawnPos().getCenter();
        AABB spawnBoundingBox = AABB.ofSize(posSpawn, diameter, diameter, diameter);

        List<Player> entities = level.getEntitiesOfClass(Player.class, spawnBoundingBox);

        List<ServerPlayer> players = level.players();
        players.forEach((ServerPlayer player) -> {if (player.getAbilities().mayfly && !entities.contains(player) && !Permissions.check(player,BYPASS_PERMISSION,2) && player.gameMode.getGameModeForPlayer() != GameType.CREATIVE && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            player.getAbilities().mayfly = false; // Remove player from mayfly
            player.onUpdateAbilities();
        }});

        entities.forEach((player) -> {
            if (player instanceof ServerPlayer) {
                var serverplayer = (ServerPlayer) player;
                int isFlyingSinceSpawn = player.getAttachedOrElse(FLYING_SINCE_SPAWN_TYPE, 0);
                var bypass = Permissions.check(player, BYPASS_PERMISSION, 2);
                var abilities = player.getAbilities();
                if (!abilities.mayfly && !bypass) { // Prevent players from getting this ability every tick
                    abilities.mayfly = true;
                    player.onUpdateAbilities(); // Tell the client they may fly
                }
                if (!bypass && abilities.flying && (CONFIG.affectCreativePlayers || serverplayer.gameMode.getGameModeForPlayer() != GameType.CREATIVE) && serverplayer.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) { // Spectator is always unaffected because they're always flying with noclip
                    abilities.flying = false; // Disable flight
                    player.onUpdateAbilities(); // Tell the client
                    player.setAttached(FLYING_SINCE_SPAWN_TYPE, 2);
                    player.startFallFlying(); // Tell player to start flying
                    Startklar.LOGGER.debug("Creative flight triggered elytra for {}", player.getName().getString());
                }
                if (player.fallDistance > CONFIG.toggleAfterFallDistanceOf && isFlyingSinceSpawn <= 0) {
                    player.setAttached(FLYING_SINCE_SPAWN_TYPE, 2);
                    player.startFallFlying();
                    Startklar.LOGGER.debug("Added FLYING_SINCE_SPAWN tag for {}", player.getUUID());
                }
            }
        });
    }

    /**
     * Determines whenever a player is allowed to keep flying without an Elytra outside spawn.
     * @see EntityElytraEvents.Custom
     */
    private static boolean keepFlying(LivingEntity entity, boolean tickElytra) {
        if (!(entity instanceof Player) || entity.level().isClientSide())
            return false;

        int isFlyingSinceSpawn = entity.getAttachedOrElse(FLYING_SINCE_SPAWN_TYPE, 0);
        return isFlyingSinceSpawn >= 1;
    }

    /**
     * Determines whenever a player should take fall damage / fly-into-wall damage.
     * @see ServerLivingEntityEvents.AllowDamage
     */
    private static boolean cancelDamageUponLanding(LivingEntity entity, DamageSource source, float amount) {
        Level level = entity.level();

        if (!(entity instanceof Player))
            return true;

        int isFlyingSinceSpawn = entity.getAttachedOrElse(FLYING_SINCE_SPAWN_TYPE, 0);

        boolean allowDamage = !(isFlyingSinceSpawn > 0)
                || (source != level.damageSources().fall() && source != level.damageSources().flyIntoWall());

        Startklar.LOGGER.debug("Allowing damage: {} for {}", allowDamage, entity.getUUID());
        return allowDamage;
    }

    /**
     * Allows players to perform a boost while flying upon call.
     */
    private static void flyingBoost(ServerLevel level, ItemStack flightDuration) {
        level.players().forEach((player) -> {
            int isFlyingSinceSpawn = player.getAttachedOrElse(FLYING_SINCE_SPAWN_TYPE, 0);

            if (isFlyingSinceSpawn < 2)
                return;

            // status indicator here
            player.displayClientMessage(Component.literal(CONFIG.boostIndicator).withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);

            if (player.isShiftKeyDown()) {
                var firework = new FireworkRocketEntity(level, flightDuration, player);
                level.addFreshEntity(firework);
                player.setAttached(FLYING_SINCE_SPAWN_TYPE, 1);
                Startklar.LOGGER.debug("Giving {} a boost", player.getUUID());
            }
        });
    }

    @SuppressWarnings("SameParameterValue")
    private static ResourceLocation of(String path) {
        return ResourceLocation.tryBuild(MOD_ID, path);
    }
}