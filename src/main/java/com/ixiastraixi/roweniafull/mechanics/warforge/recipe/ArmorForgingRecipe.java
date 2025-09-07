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

import static com.ixiastraixi.roweniafull.registry.warforge.WarforgeRecipes.ARMOR_FORGING_SERIALIZER;
import static com.ixiastraixi.roweniafull.registry.warforge.WarforgeRecipes.ARMOR_FORGING_TYPE;

public class ArmorForgingRecipe implements Recipe<Container> {

//-------------------------------------------------------
//       Объявление и привязка слотов к рецепту
//-------------------------------------------------------

    public enum Form { HELMET, CHESTPLATE, LEGGINGS, BOOTS, SHIELD }

    private final ResourceLocation id;
    private final Form form;
    private final ItemStack result;

    // Ингредиенты по слотам 0..8 (A..I)
    private final Ingredient p0;
    private final Ingredient p1;
    private final Ingredient p2;
    private final Ingredient p3;
    private final Ingredient p4;
    private final Ingredient p5;
    private final Ingredient p6;
    private final Ingredient p7;
    private final Ingredient p8;

    public ArmorForgingRecipe(ResourceLocation id, Form form, ItemStack result,
                              Ingredient p0, Ingredient p1, Ingredient p2, Ingredient p3,
                              Ingredient p4, Ingredient p5, Ingredient p6,
                              Ingredient p7, Ingredient p8) {
        this.id = id;
        this.form = form;
        this.result = result;
        this.p0 = p0 == null ? Ingredient.EMPTY : p0;
        this.p1 = p1 == null ? Ingredient.EMPTY : p1;
        this.p2 = p2 == null ? Ingredient.EMPTY : p2;
        this.p3 = p3 == null ? Ingredient.EMPTY : p3;
        this.p4 = p4 == null ? Ingredient.EMPTY : p4;
        this.p5 = p5 == null ? Ingredient.EMPTY : p5;
        this.p6 = p6 == null ? Ingredient.EMPTY : p6;
        this.p7 = p7 == null ? Ingredient.EMPTY : p7;
        this.p8 = p8 == null ? Ingredient.EMPTY : p8;
    }

    // Проверяем активные ячейки конкретной формы
    @Override
    public boolean matches(Container c, Level lvl) {
        return switch (form) {
            case HELMET ->
                       p0.test(c.getItem(0))
                    && p1.test(c.getItem(1))
                    && p2.test(c.getItem(2))
                    && p3.test(c.getItem(3))
                    && p5.test(c.getItem(5))
                    && emptyExcept(c, new int[]{0,1,2,3,5});
            case CHESTPLATE ->
                       p0.test(c.getItem(0))
                    && p2.test(c.getItem(2))
                    && p3.test(c.getItem(3))
                    && p4.test(c.getItem(4))
                    && p5.test(c.getItem(5))
                    && p6.test(c.getItem(6))
                    && p7.test(c.getItem(7))
                    && p8.test(c.getItem(8))
                    && emptyExcept(c, new int[]{0,2,3,4,5,6,7,8});
            case LEGGINGS ->
                       p0.test(c.getItem(0))
                    && p1.test(c.getItem(1))
                    && p2.test(c.getItem(2))
                    && p3.test(c.getItem(3))
                    && p5.test(c.getItem(5))
                    && p6.test(c.getItem(6))
                    && p8.test(c.getItem(8))
                    && emptyExcept(c, new int[]{0,1,2,3,5,6,8});
            case BOOTS ->
                       p3.test(c.getItem(3))
                    && p5.test(c.getItem(5))
                    && p6.test(c.getItem(6))
                    && p8.test(c.getItem(8))
                    && emptyExcept(c, new int[]{3,5,6,8});
            case SHIELD ->
                       p0.test(c.getItem(0))
                    && p1.test(c.getItem(1))
                    && p2.test(c.getItem(2))
                    && p3.test(c.getItem(3))
                    && p4.test(c.getItem(4))
                    && p5.test(c.getItem(5))
                    && p7.test(c.getItem(7))
                    && emptyExcept(c, new int[]{0,1,2,3,4,5,7});
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
    @Override public RecipeSerializer<ArmorForgingRecipe> getSerializer() { return ARMOR_FORGING_SERIALIZER.get(); }
    @Override public RecipeType<ArmorForgingRecipe> getType() { return ARMOR_FORGING_TYPE.get(); }
    public Form form() { return form; }

    // Возвращает индексы активных ячеек для каждой формы — можно использовать в других классах
    public static int[] activeIndices(Form f) {
        return switch (f) {
            case HELMET     -> new int[]{0,1,2,3,5};
            case CHESTPLATE -> new int[]{0,2,3,4,5,6,7,8};
            case LEGGINGS   -> new int[]{0,1,2,3,5,6,8};
            case BOOTS      -> new int[]{3,5,6,8};
            case SHIELD     -> new int[]{0,1,2,3,4,5,7};
        };
    }

//------------------------------
//       Сериализатор
//------------------------------

    public static class Serializer implements RecipeSerializer<ArmorForgingRecipe> {

        @Override
        public ArmorForgingRecipe fromJson(ResourceLocation id, JsonObject json) {
            // Читаем форму
            String fName = GsonHelper.getAsString(json, "form");
            Form form;
            try {
                form = Form.valueOf(fName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new JsonSyntaxException("Unknown form '" + fName + "'");
            }

            // результат
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));

            // подготавливаем ингридиенты
            Ingredient p0 = Ingredient.EMPTY, p1 = Ingredient.EMPTY, p2 = Ingredient.EMPTY,
                    p3 = Ingredient.EMPTY, p4 = Ingredient.EMPTY, p5 = Ingredient.EMPTY,
                    p6 = Ingredient.EMPTY, p7 = Ingredient.EMPTY, p8 = Ingredient.EMPTY;

            // читаем только активные позиции
            for (int idx : activeIndices(form)) {
                String key = "p" + idx;
                if (!json.has(key)) throw new JsonSyntaxException("Missing ingredient '" + key + "' for " + form);
                Ingredient ing = Ingredient.fromJson(json.get(key));
                switch (idx) {
                    case 0 -> p0 = ing;
                    case 1 -> p1 = ing;
                    case 2 -> p2 = ing;
                    case 3 -> p3 = ing;
                    case 4 -> p4 = ing;
                    case 5 -> p5 = ing;
                    case 6 -> p6 = ing;
                    case 7 -> p7 = ing;
                    case 8 -> p8 = ing;
                }
            }

            return new ArmorForgingRecipe(id, form, result, p0, p1, p2, p3, p4, p5, p6, p7, p8);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ArmorForgingRecipe r) {
            buf.writeEnum(r.form);
            buf.writeItem(r.result);
            // отправляем ингредиенты только активных ячеек
            for (int idx : activeIndices(r.form)) {
                switch (idx) {
                    case 0 -> r.p0.toNetwork(buf);
                    case 1 -> r.p1.toNetwork(buf);
                    case 2 -> r.p2.toNetwork(buf);
                    case 3 -> r.p3.toNetwork(buf);
                    case 4 -> r.p4.toNetwork(buf);
                    case 5 -> r.p5.toNetwork(buf);
                    case 6 -> r.p6.toNetwork(buf);
                    case 7 -> r.p7.toNetwork(buf);
                    case 8 -> r.p8.toNetwork(buf);
                }
            }
        }

        @Override
        public ArmorForgingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Form form = buf.readEnum(Form.class);
            ItemStack res = buf.readItem();
            Ingredient p0 = Ingredient.EMPTY, p1 = Ingredient.EMPTY, p2 = Ingredient.EMPTY,
                    p3 = Ingredient.EMPTY, p4 = Ingredient.EMPTY, p5 = Ingredient.EMPTY,
                    p6 = Ingredient.EMPTY, p7 = Ingredient.EMPTY, p8 = Ingredient.EMPTY;
            for (int idx : activeIndices(form)) {
                Ingredient ing = Ingredient.fromNetwork(buf);
                switch (idx) {
                    case 0 -> p0 = ing;
                    case 1 -> p1 = ing;
                    case 2 -> p2 = ing;
                    case 3 -> p3 = ing;
                    case 4 -> p4 = ing;
                    case 5 -> p5 = ing;
                    case 6 -> p6 = ing;
                    case 7 -> p7 = ing;
                    case 8 -> p8 = ing;
                }
            }
            return new ArmorForgingRecipe(id, form, res, p0, p1, p2, p3, p4, p5, p6, p7, p8);
        }
    }
}
