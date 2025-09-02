package com.ixiastraixi.roweniafull.mechanics.warforge.recipe;

/*
 * Rowenia / Warforge — cleaned SOT snapshot
 * File: WeaponForgingRecipe.java
 * Purpose: Recipe: forms (short/long/two-handed/polearm), matching against active slots, assemble, (de)serialization.
 * Notes:
 *  - Comments rewritten to explain code blocks and hot parameters (coords, textures, slots).
 *  - Keep this file as source of truth as of 2025-08-30 13:57:50.
 */


import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
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

import static com.ixiastraixi.roweniafull.registry.warforge.WarforgeRecipes.WEAPON_FORGING_SERIALIZER;
import static com.ixiastraixi.roweniafull.registry.warforge.WarforgeRecipes.WEAPON_FORGING_TYPE;

public class WeaponForgingRecipe implements Recipe<Container> {

    // слоты BE: 0=A,1=B,2=C,3=D,4=E(нижняя),5=FUEL,6=OUT,7=F(верхняя)

    public enum Form { SHORT, LONG, TWO_HANDED, POLEARM }

    private final ResourceLocation id;
    private final Form form;
    private final ItemStack result;

    private final Ingredient material;      // B (SHORT/POLEARM)
    private final Ingredient materialA;     // A (LONG/TWO)
    private final Ingredient materialB;     // B (LONG/TWO)
    private final Ingredient materialC;     // C (TWO)
    private final Ingredient materialD;     // D (TWO)
    private final Ingredient handle;        // E (SHORT/LONG/TWO)
    private final Ingredient handleTop;     // F (POLEARM)
    private final Ingredient handleBottom;  // E (POLEARM)

    public WeaponForgingRecipe(ResourceLocation id,
                               Form form,
                               ItemStack result,
                               Ingredient material,
                               Ingredient materialA,
                               Ingredient materialB,
                               Ingredient materialC,
                               Ingredient materialD,
                               Ingredient handle,
                               Ingredient handleTop,
                               Ingredient handleBottom) {
        this.id = id;
        this.form = form;
        this.result = result;

        this.material = material == null ? Ingredient.EMPTY : material;
        this.materialA = materialA == null ? Ingredient.EMPTY : materialA;
        this.materialB = materialB == null ? Ingredient.EMPTY : materialB;
        this.materialC = materialC == null ? Ingredient.EMPTY : materialC;
        this.materialD = materialD == null ? Ingredient.EMPTY : materialD;
        this.handle = handle == null ? Ingredient.EMPTY : handle;
        this.handleTop = handleTop == null ? Ingredient.EMPTY : handleTop;
        this.handleBottom = handleBottom == null ? Ingredient.EMPTY : handleBottom;
    }

    @Override public boolean matches(Container c, Level lvl) {
        return switch (form) {
            case SHORT ->
                    material.test(c.getItem(1)) && handle.test(c.getItem(4));
            case LONG ->
                    materialA.test(c.getItem(0)) && materialB.test(c.getItem(1)) && handle.test(c.getItem(4));
            case TWO_HANDED ->
                    materialA.test(c.getItem(0)) && materialB.test(c.getItem(1)) &&
                            materialC.test(c.getItem(2)) && materialD.test(c.getItem(3)) &&
                            handle.test(c.getItem(4));
            case POLEARM ->
                    material.test(c.getItem(1)) && handleTop.test(c.getItem(7)) && handleBottom.test(c.getItem(4));
        };
    }

    @Override public ItemStack assemble(Container c, RegistryAccess ra) { return result.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess ra) { return result; }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return WEAPON_FORGING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return WEAPON_FORGING_TYPE.get(); }

    public Form form() { return form; }

    // ---------------- Serializer ----------------

    public static class Serializer implements RecipeSerializer<WeaponForgingRecipe> {

        @Override
        public WeaponForgingRecipe fromJson(ResourceLocation id, JsonObject json) {
            // result (даём явную ошибку, если его нет)
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));

            // form: может отсутствовать — тогда пытаемся вывести из набора ключей
            Form form = readOrInferForm(id, json);

            Ingredient material = Ingredient.EMPTY;
            Ingredient materialA = Ingredient.EMPTY, materialB = Ingredient.EMPTY, materialC = Ingredient.EMPTY, materialD = Ingredient.EMPTY;
            Ingredient handle = Ingredient.EMPTY, handleTop = Ingredient.EMPTY, handleBottom = Ingredient.EMPTY;

            switch (form) {
                case SHORT -> {
                    material = readIng(json, "material", id, form);
                    handle   = readIng(json, "handle",   id, form);
                }
                case LONG -> {
                    materialA = readIng(json, "material_a", id, form);
                    materialB = readIng(json, "material_b", id, form);
                    handle    = readIng(json, "handle",     id, form);
                }
                case TWO_HANDED -> {
                    materialA = readIng(json, "material_a", id, form);
                    materialB = readIng(json, "material_b", id, form);
                    materialC = readIng(json, "material_c", id, form);
                    materialD = readIng(json, "material_d", id, form);
                    handle    = readIng(json, "handle",     id, form);
                }
                case POLEARM -> {
                    material     = readIng(json, "material",  id, form);
                    handleTop    = readIng(json, "handle_a",  id, form);
                    handleBottom = readIng(json, "handle_b",  id, form);
                }
            }

            return new WeaponForgingRecipe(id, form, result, material, materialA, materialB, materialC, materialD, handle, handleTop, handleBottom);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, WeaponForgingRecipe r) {
            buf.writeEnum(r.form);
            buf.writeItem(r.result);

            switch (r.form) {
                case SHORT -> {
                    r.material.toNetwork(buf);
                    r.handle.toNetwork(buf);
                }
                case LONG -> {
                    r.materialA.toNetwork(buf);
                    r.materialB.toNetwork(buf);
                    r.handle.toNetwork(buf);
                }
                case TWO_HANDED -> {
                    r.materialA.toNetwork(buf);
                    r.materialB.toNetwork(buf);
                    r.materialC.toNetwork(buf);
                    r.materialD.toNetwork(buf);
                    r.handle.toNetwork(buf);
                }
                case POLEARM -> {
                    r.material.toNetwork(buf);
                    r.handleTop.toNetwork(buf);
                    r.handleBottom.toNetwork(buf);
                }
            }
        }

        @Override
        public WeaponForgingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Form form = buf.readEnum(Form.class);
            ItemStack result = buf.readItem();

            Ingredient material = Ingredient.EMPTY;
            Ingredient materialA = Ingredient.EMPTY, materialB = Ingredient.EMPTY, materialC = Ingredient.EMPTY, materialD = Ingredient.EMPTY;
            Ingredient handle = Ingredient.EMPTY, handleTop = Ingredient.EMPTY, handleBottom = Ingredient.EMPTY;

            switch (form) {
                case SHORT -> {
                    material = Ingredient.fromNetwork(buf);
                    handle   = Ingredient.fromNetwork(buf);
                }
                case LONG -> {
                    materialA = Ingredient.fromNetwork(buf);
                    materialB = Ingredient.fromNetwork(buf);
                    handle    = Ingredient.fromNetwork(buf);
                }
                case TWO_HANDED -> {
                    materialA = Ingredient.fromNetwork(buf);
                    materialB = Ingredient.fromNetwork(buf);
                    materialC = Ingredient.fromNetwork(buf);
                    materialD = Ingredient.fromNetwork(buf);
                    handle    = Ingredient.fromNetwork(buf);
                }
                case POLEARM -> {
                    material     = Ingredient.fromNetwork(buf);
                    handleTop    = Ingredient.fromNetwork(buf);
                    handleBottom = Ingredient.fromNetwork(buf);
                }
            }

            return new WeaponForgingRecipe(id, form, result, material, materialA, materialB, materialC, materialD, handle, handleTop, handleBottom);
        }

        // ---- helpers ----

        private static Ingredient readIng(JsonObject json, String key, ResourceLocation id, Form f) {
            if (!json.has(key))
                throw new JsonSyntaxException("Recipe "+id+" ("+f+") missing ingredient '"+key+"'");
            return Ingredient.fromJson(json.get(key));
        }

        private static Form readOrInferForm(ResourceLocation id, JsonObject json) {
            if (GsonHelper.isStringValue(json, "form")) {
                String f = GsonHelper.getAsString(json, "form");
                return switch (f) {
                    case "short" -> Form.SHORT;
                    case "long" -> Form.LONG;
                    case "two_handed" -> Form.TWO_HANDED;
                    case "polearm" -> Form.POLEARM;
                    default -> throw new JsonSyntaxException("Recipe "+id+" has unknown form: '"+f+"'");
                };
            }

            // авто-детекция по ключам, если form не указан
            boolean hasA = json.has("material_a");
            boolean hasB = json.has("material_b");
            boolean hasC = json.has("material_c");
            boolean hasD = json.has("material_d");
            boolean hasMat = json.has("material");
            boolean hasHandle = json.has("handle");
            boolean hasHa = json.has("handle_a");
            boolean hasHb = json.has("handle_b");

            if (hasMat && hasHa && hasHb) return Form.POLEARM;
            if (hasA && hasB && hasC && hasD && hasHandle) return Form.TWO_HANDED;
            if (hasA && hasB && hasHandle) return Form.LONG;
            if (hasMat && hasHandle) return Form.SHORT;

            throw new JsonSyntaxException("Recipe "+id+" missing 'form' and cannot infer: expected keys for one of forms (short/long/two_handed/polearm)");
        }
    }
}
