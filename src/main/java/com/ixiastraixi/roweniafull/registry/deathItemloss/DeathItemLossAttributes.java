package com.ixiastraixi.roweniafull.registry.deathItemloss;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import com.ixiastraixi.roweniafull.RoweniaFull;

public final class DeathItemLossAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, RoweniaFull.MOD_ID);


    public static final RegistryObject<Attribute> ITEM_LOSS_CHANCE = ATTRIBUTES.register(
            "item_loss_chance",
            () -> new RangedAttribute("attribute.name.roweniafull.item_loss_chance", 1.0D, 0.0D, 10.0D).setSyncable(true)
    );

    private DeathItemLossAttributes() {}

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
    }


    @Mod.EventBusSubscriber(modid = RoweniaFull.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
            if (ITEM_LOSS_CHANCE.isPresent()) {
                event.add(EntityType.PLAYER, ITEM_LOSS_CHANCE.get());
            }
        }
    }
}
