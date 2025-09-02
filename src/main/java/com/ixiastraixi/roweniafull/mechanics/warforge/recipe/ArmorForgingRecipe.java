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
    public enum Form { HELMET, CHESTPLATE, LEGGINGS, BOOTS, SHIELD }

    private final ResourceLocation id;
    private final Form form;
    private final ItemStack result;
    private final Ingredient[] slotIng = new Ingredient[9]; // p0..p8

    public ArmorForgingRecipe(ResourceLocation id, Form form, ItemStack result, Ingredient[] slotIng) {
        this.id = id; this.form = form; this.result = result;
        for (int i=0;i<9;i++) this.slotIng[i] = (slotIng[i]==null)? Ingredient.EMPTY : slotIng[i];
    }

    @Override public boolean matches(Container c, Level lvl) {
        int[] act = activeIndices(form);
        for (int idx: act) if (!slotIng[idx].test(c.getItem(idx))) return false;
        // вне активных позиций — пусто
        outer:
        for (int i=0;i<9;i++) {
            for (int a:act) if (a==i) continue outer;
            if (!c.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override public ItemStack assemble(Container c, RegistryAccess ra) { return result.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess ra) { return result; }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ARMOR_FORGING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ARMOR_FORGING_TYPE.get(); }
    public Form form() { return form; }

    public static int[] activeIndices(Form f) {
        return switch (f) {
            case HELMET     -> new int[]{0,1,2,3,5};
            case CHESTPLATE -> new int[]{0,2,3,4,5,6,7,8};
            case LEGGINGS   -> new int[]{0,1,2,3,5,6,8};
            case BOOTS      -> new int[]{3,5,6,8};
            case SHIELD     -> new int[]{0,1,2,3,4,5,7};
        };
    }

    // --- сериализатор (JSON/Net) ---
    public static class Serializer implements RecipeSerializer<ArmorForgingRecipe> {
        @Override public ArmorForgingRecipe fromJson(ResourceLocation id, JsonObject json) {
            String fName = GsonHelper.getAsString(json, "form");
            Form form;
            try { form = Form.valueOf(fName.toUpperCase()); }
            catch (IllegalArgumentException ex) { throw new JsonSyntaxException("Unknown form '"+fName+"'"); }

            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            Ingredient[] ing = new Ingredient[9]; for (int i=0;i<9;i++) ing[i] = Ingredient.EMPTY;
            for (int idx: activeIndices(form)) {
                String key = "p"+idx;
                if (!json.has(key)) throw new JsonSyntaxException("Missing ingredient '"+key+"' for "+form);
                ing[idx] = Ingredient.fromJson(json.get(key));
            }
            return new ArmorForgingRecipe(id, form, result, ing);
        }

        @Override public ArmorForgingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Form form = buf.readEnum(Form.class);
            ItemStack res = buf.readItem();
            Ingredient[] ing = new Ingredient[9]; for (int i=0;i<9;i++) ing[i] = Ingredient.EMPTY;
            int[] act = activeIndices(form);
            for (int idx: act) ing[idx] = Ingredient.fromNetwork(buf);
            return new ArmorForgingRecipe(id, form, res, ing);
        }

        @Override public void toNetwork(FriendlyByteBuf buf, ArmorForgingRecipe r) {
            buf.writeEnum(r.form);
            buf.writeItem(r.result);
            int[] act = activeIndices(r.form);
            for (int idx: act) r.slotIng[idx].toNetwork(buf);
        }
    }
}
