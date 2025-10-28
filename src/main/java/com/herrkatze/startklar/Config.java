package com.herrkatze.startklar;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.FloatRange;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;

public class Config extends WrappedConfig {
    @Comment("This message is being shown to the player when they have not used their boost yet midair.")
    public String boostIndicator = "ᴘʀᴇss [sʜɪғᴛ] ғᴏʀ ᴀ ʙᴏᴏsᴛ!";

    @Comment("The spawn range where players can start flying, measured in blocks.\n" +
            "This is a box with the world spawn at the center.")
    public int spawnDiameter = 32;

    @Comment("Determines after how many blocks of fall distance the fly mode should be auto-toggled.")
    @FloatRange(min = 0.0F, max = 256.0F)
    public float toggleAfterFallDistanceOf = 3.0F;

    @Comment("The flight duration of the boost. Setting it to 0 means the boost is disabled.")
    @IntegerRange(min = 0, max = 3)
    public int flightDuration = 1;

    @Comment("Whether or not the double tap to launch applies to creative players")
    public boolean affectCreativePlayers = false;
}