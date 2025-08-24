package com.mineposteurs;

import org.slf4j.Logger;

import com.mineposteurs.events.AnnonceHandler;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.WritableBookItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

@Mod(LesMineposteurs.MODID)
public class LesMineposteurs {
    public static final String MODID = "lesmineposteurs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LesMineposteurs(ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(new AnnonceHandler());
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}