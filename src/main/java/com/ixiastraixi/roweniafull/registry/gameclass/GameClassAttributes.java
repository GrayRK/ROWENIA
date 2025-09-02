package com.ixiastraixi.roweniafull.registry.gameclass;

import com.ixiastraixi.roweniafull.RoweniaFull;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Minecraft 1.20.1 (official mappings), Forge 47.4.0
 * Бинарные атрибуты игровых классов: 0.0 = нет класса, 1.0 = класс выдан.
 * Атрибуты синхронизируются на клиент.
 */
public final class GameClassAttributes {
    private GameClassAttributes() {}

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, RoweniaFull.MOD_ID);

    // === Классы ===
    public static final RegistryObject<Attribute> CLASS_WARRIOR   = registerClass("warrior");   // Воин
    public static final RegistryObject<Attribute> CLASS_HUNTER    = registerClass("hunter");    // Охотник
    public static final RegistryObject<Attribute> CLASS_SORCERER  = registerClass("sorcerer");  // Чародей
    public static final RegistryObject<Attribute> CLASS_WIZARD    = registerClass("wizard");    // Волшебник
    public static final RegistryObject<Attribute> CLASS_COOK      = registerClass("cook");      // Повар
    public static final RegistryObject<Attribute> CLASS_MINER     = registerClass("miner");     // Горняк

    /**
     * Регистрация реестра атрибутов в шину событий (вызвать из конструктора/инициализации мода).
     */
    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
    }

    // --- Утилиты для проверки на сущности (значение >= 1.0 считается "выдан класс") ---

    public static boolean isWarrior(LivingEntity entity)  { return hasClass(entity, CLASS_WARRIOR); }
    public static boolean isHunter(LivingEntity entity)   { return hasClass(entity, CLASS_HUNTER); }
    public static boolean isSorcerer(LivingEntity entity) { return hasClass(entity, CLASS_SORCERER); }
    public static boolean isWizard(LivingEntity entity)   { return hasClass(entity, CLASS_WIZARD); }
    public static boolean isCook(LivingEntity entity)     { return hasClass(entity, CLASS_COOK); }
    public static boolean isMiner(LivingEntity entity)    { return hasClass(entity, CLASS_MINER); }

    // === Внутреннее ===

    private static RegistryObject<Attribute> registerClass(String key) {
        // id атрибута в реестре: roweniafull:class.<key>
        // локализационный ключ: attribute.name.roweniafull.class.<key>
        return ATTRIBUTES.register("class." + key,
                () -> new RangedAttribute("attribute.name." + RoweniaFull.MOD_ID + ".class." + key,
                        0.0D, 0.0D, 1.0D).setSyncable(true));
    }

    private static boolean hasClass(LivingEntity entity, RegistryObject<Attribute> attr) {
        if (entity == null || attr == null || !attr.isPresent()) return false;
        AttributeInstance inst = entity.getAttribute(attr.get());
        return inst != null && inst.getValue() >= 1.0D;
    }
}
