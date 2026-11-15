package com.example.createftbguard;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CreateFtbGuardMod.MOD_ID)
public class CreateFtbGuardMod {
    public static final String MOD_ID = "createftbguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CreateFtbGuardMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, GuardConfig.SPEC, "createftbguard-server.toml");
        NeoForge.EVENT_BUS.register(new GuardEventHandler());
        LOGGER.info("Create FTB Chunks Guard initialized (Server-side protection & Anti-Dupe active).");
    }
}
