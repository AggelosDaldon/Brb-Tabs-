package gr.daldon.brbtabs.mixin;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Crafting-recipe pages indexed by the real Creative Mode registry, including
 * modded tabs. Requires the actual 1.20.1 BRB mod to be installed.
 */
@Mixin(value = RecipeBookComponent.class, priority = 2000)
public abstract class RecipeBookTabsMixin {
    @Shadow private int width;
    @Shadow private int height;
    @Shadow private int xOffset;
    @Shadow protected Minecraft minecraft;
    @Shadow private ClientRecipeBook book;
    @Shadow private String lastSearch;
    @Shadow protected RecipeBookMenu<?> menu;
    @Shadow @Final private RecipeBookPage recipeBookPage;

    @Unique private final List<CreativeModeTab> brbtabs$tabs = new ArrayList<>();
    @Unique private int brbtabs$selected = 0;
    @Unique private int brbtabs$page = 0;
    @Unique private static final int BRBTABS_PAGE_SIZE = 5;

    @Unique
    private boolean brbtabs$crafting() {
        return menu instanceof CraftingMenu || menu instanceof InventoryMenu;
    }

    @Unique
    private void brbtabs$rebuildTabs() {
        ResourceLocation previous = brbtabs$tabs.isEmpty()
            ? null : BuiltInRegistries.CREATIVE_MODE_TAB.getKey(brbtabs$tabs.get(Math.min(brbtabs$selected, brbtabs$tabs.size() - 1)));
        brbtabs$tabs.clear();
        for (CreativeModeTab tab : net.minecraftforge.common.CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
            if (tab.getType() == CreativeModeTab.Type.CATEGORY) {
                brbtabs$tabs.add(tab);
            }
        }
        if (previous != null) {
            for (int i = 0; i < brbtabs$tabs.size(); i++) {
                if (previous.equals(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(brbtabs$tabs.get(i)))) {
                    brbtabs$selected = i;
                    return;
                }
            }
        }
        brbtabs$selected = 0;
        brbtabs$page = 0;
    }

    @Inject(method = "initVisuals", at = @At("TAIL"))
    private void brbtabs$initTabs(CallbackInfo ci) {
        if (!brbtabs$crafting()) return;
        brbtabs$rebuildTabs();
        brbtabs$refresh(false);
    }

    @Unique
    private void brbtabs$refresh(boolean resetPage) {
        if (!brbtabs$crafting() || brbtabs$tabs.isEmpty() || minecraft == null || minecraft.player == null || book == null) return;
        if (brbtabs$selected >= brbtabs$tabs.size()) brbtabs$selected = 0;

        CreativeModeTab tab = brbtabs$tabs.get(brbtabs$selected);
        Set<ResourceLocation> accepted = new HashSet<>();
        for (ItemStack stack : tab.getDisplayItems()) {
            if (!stack.isEmpty()) accepted.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }

        List<RecipeCollection> entries = new ArrayList<>();
        for (RecipeCollection group : book.getCollection(RecipeBookCategories.CRAFTING_SEARCH)) {
            for (Recipe<?> recipe : group.getRecipes()) {
                ItemStack result = recipe.getResultItem(group.registryAccess());
                if (result.isEmpty() || !accepted.contains(BuiltInRegistries.ITEM.getKey(result.getItem()))) continue;
                String query = lastSearch == null ? "" : lastSearch.strip().toLowerCase(java.util.Locale.ROOT);
                if (!query.isEmpty() && !result.getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains(query)
                    && !BuiltInRegistries.ITEM.getKey(result.getItem()).toString().contains(query)) continue;
                // Retain the identity of every recipe. BRB supports ungrouped recipes.
                RecipeCollection single = new RecipeCollection(group.registryAccess(), List.of(recipe));
                single.updateKnownRecipes(book);
                if (!book.isFiltering(menu) || single.hasCraftable()) {
                    entries.add(single);
                }
            }
        }
        recipeBookPage.updateCollections(entries, resetPage);
    }

    // Reapply the tab filter after searches or inventory updates. Called before
    // BRB's own updateCollections injections can replace the standard result.
    @Inject(method = "updateCollections", at = @At("HEAD"), cancellable = true)
    private void brbtabs$filter(boolean resetPage, CallbackInfo ci) {
        if (!brbtabs$crafting() || brbtabs$tabs.isEmpty()) return;
        brbtabs$refresh(resetPage);
        ci.cancel();
    }

    @Unique
    private int brbtabs$left() {
        return (width - 147) / 2 - xOffset - 27;
    }

    @Unique
    private int brbtabs$top() {
        return (height - 166) / 2 + 8;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void brbtabs$renderTabs(GuiGraphics gui, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!brbtabs$crafting() || brbtabs$tabs.isEmpty()) return;
        int x = brbtabs$left();
        int y = brbtabs$top();
        int begin = brbtabs$page * BRBTABS_PAGE_SIZE;
        for (int i = begin; i < Math.min(begin + BRBTABS_PAGE_SIZE, brbtabs$tabs.size()); i++) {
            int pos = i - begin;
            int ty = y + 21 + pos * 23;
            boolean selected = i == brbtabs$selected;
            boolean hovered = mouseX >= x && mouseX < x + 24 && mouseY >= ty && mouseY < ty + 22;
            gui.fill(x, ty, x + 24, ty + 22, selected ? 0xFFE0D5B9 : hovered ? 0xFFB5B09E : 0xFF77766F);
            gui.fill(x + 1, ty + 1, x + 23, ty + 21, selected ? 0xFF9E8B69 : 0xFF45453F);
            gui.renderItem(brbtabs$tabs.get(i).getIconItem(), x + 4, ty + 3);
        }
        gui.fill(x, y, x + 24, y + 19, 0xFF45453F);
        gui.drawCenteredString(minecraft.font, "▲", x + 12, y + 5, 0xFFFFFFFF);
        int bottom = y + 138;
        gui.fill(x, bottom, x + 24, bottom + 19, 0xFF45453F);
        gui.drawCenteredString(minecraft.font, "▼", x + 12, bottom + 5, 0xFFFFFFFF);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void brbtabs$click(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!brbtabs$crafting() || brbtabs$tabs.isEmpty() || button != 0) return;
        int x = brbtabs$left();
        int y = brbtabs$top();
        if (mouseX < x || mouseX >= x + 24) return;
        if (mouseY >= y && mouseY < y + 19) {
            brbtabs$page = Math.max(0, brbtabs$page - 1);
            cir.setReturnValue(true);
            return;
        }
        if (mouseY >= y + 138 && mouseY < y + 157) {
            brbtabs$page = Math.min((brbtabs$tabs.size() - 1) / BRBTABS_PAGE_SIZE, brbtabs$page + 1);
            cir.setReturnValue(true);
            return;
        }
        int begin = brbtabs$page * BRBTABS_PAGE_SIZE;
        for (int i = begin; i < Math.min(begin + BRBTABS_PAGE_SIZE, brbtabs$tabs.size()); i++) {
            int ty = y + 21 + (i - begin) * 23;
            if (mouseY >= ty && mouseY < ty + 22) {
                brbtabs$selected = i;
                brbtabs$refresh(true);
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "renderTooltip", at = @At("TAIL"))
    private void brbtabs$tooltip(GuiGraphics gui, int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
        if (!brbtabs$crafting() || brbtabs$tabs.isEmpty()) return;
        int left = brbtabs$left();
        int top = brbtabs$top();
        if (mouseX < left || mouseX >= left + 24) return;
        int begin = brbtabs$page * BRBTABS_PAGE_SIZE;
        for (int i = begin; i < Math.min(begin + BRBTABS_PAGE_SIZE, brbtabs$tabs.size()); i++) {
            int ty = top + 21 + (i - begin) * 23;
            if (mouseY >= ty && mouseY < ty + 22) {
                gui.renderTooltip(minecraft.font, brbtabs$tabs.get(i).getDisplayName(), mouseX, mouseY);
                return;
            }
        }
    }
}
