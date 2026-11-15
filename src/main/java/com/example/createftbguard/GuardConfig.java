package com.example.createftbguard;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public class GuardConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_MOD;
    public static final ModConfigSpec.BooleanValue ALLOW_ALLIES;
    public static final ModConfigSpec.BooleanValue ALLOW_WILDERNESS_FROM_CLAIM;
    public static final ModConfigSpec.BooleanValue BLOCK_TRAINS_IN_CLAIMS;
    public static final ModConfigSpec.BooleanValue BLOCK_AIRSHIPS_IN_CLAIMS;
    public static final ModConfigSpec.BooleanValue ENABLE_ANTI_DUPE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_CREATE_BLOCKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> AIRSHIP_ENTITY_IDS;

    static {
        BUILDER.push("general");
        ENABLE_MOD = BUILDER
                .comment("Enable or disable the entire Create FTB Chunks protection.")
                .define("enableMod", true);

        ALLOW_ALLIES = BUILDER
                .comment("Allow contraptions owned by team allies to enter/interact with claimed chunks.")
                .define("allowAllies", true);

        ALLOW_WILDERNESS_FROM_CLAIM = BUILDER
                .comment("Allow contraptions in claimed chunks to interact with unclaimed wilderness chunks.")
                .define("allowWildernessFromClaim", true);
        BUILDER.pop();

        BUILDER.push("entities_and_blocks");
        BLOCK_TRAINS_IN_CLAIMS = BUILDER
                .comment("Block Create Trains from entering or moving inside foreign claimed chunks.")
                .define("blockTrainsInClaims", true);

        BLOCK_AIRSHIPS_IN_CLAIMS = BUILDER
                .comment("Block Create Aeronautics / Valkyrien Skies airships from entering foreign claimed chunks.")
                .define("blockAirshipsInClaims", true);

        BLOCKED_CREATE_BLOCKS = BUILDER
                .comment("List of Create kinetic/contraption block IDs blocked from operating across foreign claim borders.")
                .defineListAllowEmpty("blockedCreateBlocks", List.of(
                        "create:mechanical_bearing",
                        "create:clockwork_bearing",
                        "create:windmill_bearing",
                        "create:gantry_carriage",
                        "create:gantry_shaft",
                        "create:mechanical_piston",
                        "create:sticky_mechanical_piston",
                        "create:deployer",
                        "create:mechanical_drill",
                        "create:mechanical_saw",
                        "create:mechanical_harvester",
                        "create:mechanical_pump",
                        "create:item_drain"
                ), obj -> obj instanceof String);

        AIRSHIP_ENTITY_IDS = BUILDER
                .comment("Entity IDs or registry prefixes treated as airships (Create Aeronautics / VS2).")
                .defineListAllowEmpty("airshipEntityIds", List.of(
                        "createaeronautics:airship",
                        "valkyrienskies:ship"
                ), obj -> obj instanceof String);
        BUILDER.pop();

        BUILDER.push("anti_dupe");
        ENABLE_ANTI_DUPE = BUILDER
                .comment("Enable anti-dupe checks: closes inventories on chunk/contraption disassembly and prevents duplicate entity UUIDs.")
                .define("enableAntiDupe", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
