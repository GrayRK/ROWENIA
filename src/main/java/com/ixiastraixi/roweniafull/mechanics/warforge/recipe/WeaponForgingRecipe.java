package com.ixiastraixi.roweniafull.mechanics.warforge.recipe;

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

//-------------------------------------------------------
//       Объявление и привязка слотов к рецепту
//-------------------------------------------------------

    public enum Form { SHORT, LONG, TWO_HANDED, POLEARM }

    private final ResourceLocation id;
    private final Form form;
    private final ItemStack result;

    private final Ingredient m0;     // A (LONG/TWO)
    private final Ingredient m1;      // B (SHORT/POLEARM)
    private final Ingredient m2;     // B (LONG/TWO)
    private final Ingredient m3;     // C (TWO)
    private final Ingredient m4;     // D (TWO)
    private final Ingredient h0;        // E (SHORT/LONG/TWO)
    private final Ingredient h1;  // E (POLEARM)
    private final Ingredient h2;     // F (POLEARM)


    public WeaponForgingRecipe(ResourceLocation id,
                               Form form,
                               ItemStack result,
                               Ingredient m1,
                               Ingredient m0,
                               Ingredient m2,
                               Ingredient m3,
                               Ingredient m4,
                               Ingredient h0,
                               Ingredient h2,
                               Ingredient h1) {
        this.id = id;
        this.form = form;
        this.result = result;
        this.m1 = m1 == null ? Ingredient.EMPTY : m1;
        this.m0 = m0 == null ? Ingredient.EMPTY : m0;
        this.m2 = m2 == null ? Ingredient.EMPTY : m2;
        this.m3 = m3 == null ? Ingredient.EMPTY : m3;
        this.m4 = m4 == null ? Ingredient.EMPTY : m4;
        this.h0 = h0 == null ? Ingredient.EMPTY : h0;
        this.h2 = h2 == null ? Ingredient.EMPTY : h2;
        this.h1 = h1 == null ? Ingredient.EMPTY : h1;
    }

    // Проверяем активные ячейки конкретной формы
    @Override public boolean matches(Container c, Level lvl) {
        return switch (form) {
            case SHORT ->
                       m1.test(c.getItem(1))
                    && h0.test(c.getItem(4))
                    && emptyExcept(c, new int[]{1,4});
            case LONG ->
                       m0.test(c.getItem(0))
                    && m2.test(c.getItem(1))
                    && h0.test(c.getItem(4))
                    && emptyExcept(c, new int[]{0,1,4});
            case TWO_HANDED ->
                       m0.test(c.getItem(0))
                    && m2.test(c.getItem(1))
                    && m3.test(c.getItem(2))
                    && m4.test(c.getItem(3))
                    && h0.test(c.getItem(4))
                    && emptyExcept(c, new int[]{0,1,2,3,4});
            case POLEARM ->
                    m1.test(c.getItem(1))
                    && h2.test(c.getItem(7))
                    && h1.test(c.getItem(4))
                    && emptyExcept(c, new int[]{1,7,4});
        };
    }

    // Убедиться, что во всех ячейках, кроме перечисленных, ничего нет
    private boolean emptyExcept(Container c, int[] act) {
        outer:
        for (int i = 0; i < 9; i++) {
            for (int a : act) if (a == i) continue outer;
            if (!c.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override public ItemStack assemble(Container c, RegistryAccess ra) { return result.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess ra) { return result; }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return WEAPON_FORGING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return WEAPON_FORGING_TYPE.get(); }
    public Form form() { return form; }

    // Возвращает индексы активных ячеек для каждой формы — можно использовать в других классах
    public static int[] activeIndices(WeaponForgingRecipe.Form f) {
        return switch (f) {
            case SHORT      -> new int[]{1,4};
            case LONG       -> new int[]{0,1,4};
            case TWO_HANDED -> new int[]{0,1,2,3,4};
            case POLEARM    -> new int[]{1,7,4};
        };
    }

//------------------------------
//       Сериализатор
//------------------------------

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
                    r.m1.toNetwork(buf);
                    r.h0.toNetwork(buf);
                }
                case LONG -> {
                    r.m0.toNetwork(buf);
                    r.m2.toNetwork(buf);
                    r.h0.toNetwork(buf);
                }
                case TWO_HANDED -> {
                    r.m0.toNetwork(buf);
                    r.m2.toNetwork(buf);
                    r.m3.toNetwork(buf);
                    r.m4.toNetwork(buf);
                    r.h0.toNetwork(buf);
                }
                case POLEARM -> {
                    r.m1.toNetwork(buf);
                    r.h2.toNetwork(buf);
                    r.h1.toNetwork(buf);
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
