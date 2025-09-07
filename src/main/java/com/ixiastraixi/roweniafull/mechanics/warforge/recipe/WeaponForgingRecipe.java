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
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Рецепт ковки оружия для Warforge. Полностью аналогичен ArmorForgingRecipe,
 * но использует шесть именованных ингредиентов и собственные формы.
 */
public class WeaponForgingRecipe implements Recipe<Container> {

    /** Возможные формы (вкладки) кузницы оружия. */
    public enum Form { SHORT, LONG, TWO_HANDED, POLEARM }

    private final ResourceLocation id;
    private final Form form;
    private final ItemStack result;

    // Ингредиенты для разных частей: клинки (A–D) и рукояти (E–F)
    private final Ingredient A_Blade;
    private final Ingredient B_Blade;
    private final Ingredient C_Blade;
    private final Ingredient D_Blade;
    private final Ingredient E_Handle;
    private final Ingredient F_Handle;

    public WeaponForgingRecipe(ResourceLocation id,
                               Form form,
                               ItemStack result,
                               Ingredient A_Blade,
                               Ingredient B_Blade,
                               Ingredient C_Blade,
                               Ingredient D_Blade,
                               Ingredient E_Handle,
                               Ingredient F_Handle) {
        this.id = id;
        this.form = form;
        this.result = result.copy();
        // Любой null трактуем как пустой ингридиент
        this.A_Blade  = A_Blade  == null ? Ingredient.EMPTY : A_Blade;
        this.B_Blade  = B_Blade  == null ? Ingredient.EMPTY : B_Blade;
        this.C_Blade  = C_Blade  == null ? Ingredient.EMPTY : C_Blade;
        this.D_Blade  = D_Blade  == null ? Ingredient.EMPTY : D_Blade;
        this.E_Handle = E_Handle == null ? Ingredient.EMPTY : E_Handle;
        this.F_Handle = F_Handle == null ? Ingredient.EMPTY : F_Handle;
    }

    @Override public ResourceLocation getId() { return id; }
    public Form form() { return form; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return com.ixiastraixi.roweniafull.registry.warforge.WarforgeRecipes.WEAPON_FORGING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return com.ixiastraixi.roweniafull.registry.warforge.WarforgeRecipes.WEAPON_FORGING_TYPE.get();
    }

    @Override
    public boolean matches(Container c, Level level) {
        // Проверяем активные слоты и убеждаемся, что остальные пусты
        return switch (form) {
            case SHORT -> B_Blade.test(c.getItem(1))
                    && E_Handle.test(c.getItem(4))
                    && emptyExcept(c, new int[]{1,4});
            case LONG -> A_Blade.test(c.getItem(0))
                    && B_Blade.test(c.getItem(1))
                    && E_Handle.test(c.getItem(4))
                    && emptyExcept(c, new int[]{0,1,4});
            case TWO_HANDED -> A_Blade.test(c.getItem(0))
                    && B_Blade.test(c.getItem(1))
                    && C_Blade.test(c.getItem(2))
                    && D_Blade.test(c.getItem(3))
                    && E_Handle.test(c.getItem(4))
                    && emptyExcept(c, new int[]{0,1,2,3,4});
            case POLEARM -> B_Blade.test(c.getItem(1))
                    && F_Handle.test(c.getItem(5))
                    && E_Handle.test(c.getItem(4))
                    && emptyExcept(c, new int[]{1,5,4});
        };
    }

    /**
     * Проверяет, что во всех слотах, кроме перечисленных, нет предметов.
     */
    private boolean emptyExcept(Container c, int[] act) {
        outer:
        for (int i = 0; i < 9; i++) {
            for (int a : act) {
                if (a == i) continue outer;
            }
            if (!c.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    /**
     * Возвращает индексы активных ячеек рецепта для каждой формы. Может пригодиться в меню.
     */
    public static int[] activeIndices(Form f) {
        return switch (f) {
            case SHORT      -> new int[]{1,4};
            case LONG       -> new int[]{0,1,4};
            case TWO_HANDED -> new int[]{0,1,2,3,4};
            case POLEARM    -> new int[]{1,5,4};
        };
    }

    @Override public ItemStack assemble(Container c, RegistryAccess ra) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess ra) { return result.copy(); }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        // только непустые ингредиенты (для JEI)
        if (!A_Blade.isEmpty()) list.add(A_Blade);
        if (!B_Blade.isEmpty()) list.add(B_Blade);
        if (!C_Blade.isEmpty()) list.add(C_Blade);
        if (!D_Blade.isEmpty()) list.add(D_Blade);
        if (!E_Handle.isEmpty()) list.add(E_Handle);
        if (!F_Handle.isEmpty()) list.add(F_Handle);
        return list;
    }

    /**
     * Сериализатор рецепта. Читает форму, результат и необходимые слоты из JSON,
     * отправляет/получает через сеть только активные ингредиенты.
     */
    public static class Serializer implements RecipeSerializer<WeaponForgingRecipe> {

        @Override
        public WeaponForgingRecipe fromJson(ResourceLocation id, JsonObject json) {
            // форма
            Form form = Form.valueOf(GsonHelper.getAsString(json, "form").toUpperCase());

            // результат
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));

            // по умолчанию пусто
            Ingredient A = Ingredient.EMPTY, B = Ingredient.EMPTY, C = Ingredient.EMPTY;
            Ingredient D = Ingredient.EMPTY, E = Ingredient.EMPTY, F = Ingredient.EMPTY;

            // читаем только необходимые для этой формы слоты
            for (int idx : activeIndices(form)) {
                String key = switch (idx) {
                    case 0 -> "A_Blade";
                    case 1 -> "B_Blade";
                    case 2 -> "C_Blade";
                    case 3 -> "D_Blade";
                    case 4 -> "E_Handle";
                    case 5 -> "F_Handle";
                    default -> null;
                };
                if (key == null || !json.has(key)) {
                    throw new IllegalArgumentException("Missing required ingredient '" + key + "' for form " + form);
                }
                Ingredient ing = Ingredient.fromJson(json.get(key));
                switch (idx) {
                    case 0 -> A = ing;
                    case 1 -> B = ing;
                    case 2 -> C = ing;
                    case 3 -> D = ing;
                    case 4 -> E = ing;
                    case 5 -> F = ing;
                }
            }

            return new WeaponForgingRecipe(id, form, result, A, B, C, D, E, F);
        }

        @Nullable
        @Override
        public WeaponForgingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Form form = buf.readEnum(Form.class);
            ItemStack result = buf.readItem();

            Ingredient A = Ingredient.EMPTY, B = Ingredient.EMPTY, C = Ingredient.EMPTY;
            Ingredient D = Ingredient.EMPTY, E = Ingredient.EMPTY, F = Ingredient.EMPTY;
            for (int idx : activeIndices(form)) {
                Ingredient ing = Ingredient.fromNetwork(buf);
                switch (idx) {
                    case 0 -> A = ing;
                    case 1 -> B = ing;
                    case 2 -> C = ing;
                    case 3 -> D = ing;
                    case 4 -> E = ing;
                    case 5 -> F = ing;
                }
            }
            return new WeaponForgingRecipe(id, form, result, A, B, C, D, E, F);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, WeaponForgingRecipe recipe) {
            buf.writeEnum(recipe.form);
            buf.writeItem(recipe.result);
            // отправляем только активные ингредиенты
            for (int idx : activeIndices(recipe.form)) {
                switch (idx) {
                    case 0 -> recipe.A_Blade.toNetwork(buf);
                    case 1 -> recipe.B_Blade.toNetwork(buf);
                    case 2 -> recipe.C_Blade.toNetwork(buf);
                    case 3 -> recipe.D_Blade.toNetwork(buf);
                    case 4 -> recipe.E_Handle.toNetwork(buf);
                    case 5 -> recipe.F_Handle.toNetwork(buf);
                }
            }
        }
    }
}
