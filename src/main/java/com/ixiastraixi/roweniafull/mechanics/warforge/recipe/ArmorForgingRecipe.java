package com.ixiastraixi.roweniafull.mechanics.warforge.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import com.ixiastraixi.roweniafull.registry.warforge.WarforgeRecipes;

import java.util.Locale;

/**
 * Унифицированный рецепт для ArmorForge.
 * Слоты входа: 0..8 (сетка 3×3). Используемые индексы зависят от формы (табов).
 * JSON-ключи позиций: p0..p8.
 */
public class ArmorForgingRecipe implements Recipe<Container> {

    // ----- общая часть (идентична классу оружия) -----
    private final ResourceLocation id;
    private final Form form;
    private final ItemStack result;
    private final Ingredient[] p = new Ingredient[9];

    public ArmorForgingRecipe(ResourceLocation id, Form form, ItemStack result,
                              Ingredient p0, Ingredient p1, Ingredient p2,
                              Ingredient p3, Ingredient p4, Ingredient p5,
                              Ingredient p6, Ingredient p7, Ingredient p8) {
        this.id = id;
        this.form = form;
        this.result = result.copy();
        this.p[0] = p0 == null ? Ingredient.EMPTY : p0;
        this.p[1] = p1 == null ? Ingredient.EMPTY : p1;
        this.p[2] = p2 == null ? Ingredient.EMPTY : p2;
        this.p[3] = p3 == null ? Ingredient.EMPTY : p3;
        this.p[4] = p4 == null ? Ingredient.EMPTY : p4;
        this.p[5] = p5 == null ? Ingredient.EMPTY : p5;
        this.p[6] = p6 == null ? Ingredient.EMPTY : p6;
        this.p[7] = p7 == null ? Ingredient.EMPTY : p7;
        this.p[8] = p8 == null ? Ingredient.EMPTY : p8;
    }

    // --- enum форм (вкладок) для брони ---
    public enum Form {
        HELMET, CHESTPLATE, LEGGINGS, BOOTS, SHIELD;

        public static Form byName(String s) {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        }
    }

    /** Возвращает индексы активных слотов (0..8) для конкретной формы. */
    public static int[] activeIndices(Form f) {
        return switch (f) {
            case HELMET     -> new int[]{0,1,2,3,5};
            case CHESTPLATE -> new int[]{0,2,3,4,5,6,7,8};
            case LEGGINGS   -> new int[]{0,1,2,3,5,6,8};
            case BOOTS      -> new int[]{3,5,6,8};
            case SHIELD     -> new int[]{0,1,2,3,4,5,7};
        };
    }

    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return WarforgeRecipes.ARMOR_FORGING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return WarforgeRecipes.ARMOR_FORGING_TYPE.get(); }

    public Form form() { return form; }

    @Override
    public boolean matches(Container cont, Level level) {
        if (cont.getContainerSize() < 9) return false;

        for (int idx : activeIndices(form)) {
            Ingredient req = p[idx];
            ItemStack in = cont.getItem(idx);
            if (req.isEmpty()) {
                if (!in.isEmpty()) return false;
            } else {
                if (!req.test(in)) return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Container cont, RegistryAccess regs) {
        return result.copy();
    }

    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess regs) { return result.copy(); }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.withSize(9, Ingredient.EMPTY);
        for (int i = 0; i < 9; i++) list.set(i, p[i]);
        return list;
    }

    // ----- сериализатор (идентичен Weapon) -----
    public static class Serializer implements RecipeSerializer<ArmorForgingRecipe> {

        @Override
        public ArmorForgingRecipe fromJson(ResourceLocation id, JsonObject obj) {
            Form form = Form.byName(GsonHelper.getAsString(obj, "form"));
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(obj, "result"));

            Ingredient[] pos = new Ingredient[9];
            for (int i = 0; i < 9; i++) {
                String key = "p" + i;
                pos[i] = obj.has(key) ? Ingredient.fromJson(obj.get(key)) : Ingredient.EMPTY;
            }
            return new ArmorForgingRecipe(id, form, result,
                    pos[0], pos[1], pos[2], pos[3], pos[4], pos[5], pos[6], pos[7], pos[8]);
        }

        @Override
        public ArmorForgingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Form form = buf.readEnum(Form.class);
            ItemStack result = buf.readItem();
            Ingredient[] pos = new Ingredient[9];
            for (int i = 0; i < 9; i++) pos[i] = Ingredient.fromNetwork(buf);
            return new ArmorForgingRecipe(id, form, result,
                    pos[0], pos[1], pos[2], pos[3], pos[4], pos[5], pos[6], pos[7], pos[8]);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ArmorForgingRecipe r) {
            buf.writeEnum(r.form);
            buf.writeItem(r.result);
            for (int i = 0; i < 9; i++) r.p[i].toNetwork(buf);
        }
    }
}
