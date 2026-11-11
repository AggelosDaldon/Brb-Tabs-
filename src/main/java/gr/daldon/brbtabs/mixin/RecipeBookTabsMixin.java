package gr.daldon.brbtabs.mixin;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
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
    @Shadow @Final private List<RecipeBookTabButton> tabButtons;

    @Unique private final List<CreativeModeTab> brbtabs$tabs = new ArrayList<>();
    @Unique private int brbtabs$selected = 0;
    @Unique private int brbtabs$page = 0;
    @Unique private boolean brbtabs$searchAll = false;
    @Unique private static final ResourceLocation BRBTABS_VANILLA_BOOK = new ResourceLocation("minecraft", "textures/gui/recipe_book.png");
    @Unique private static final int BRBTABS_PAGE_SIZE = 10;
    @Unique private static final int BRBTABS_COLUMNS = 2;
    @Unique private static final int BRBTABS_CELL = 22;

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
        brbtabs$hideOriginalTabs();
        brbtabs$refresh(false);
    }

    // Keep vanilla/BRB category buttons in the component so vanilla state and
    // other BRB mixins continue to work; only disable their visual hit areas.
    // Removing them from the list risks breaking vanilla's selected tab logic.
    @Unique
    private void brbtabs$hideOriginalTabs() {
        if (!brbtabs$crafting()) return;
        for (RecipeBookTabButton tab : tabButtons) {
            tab.visible = false;
        }
    }

    // Other mods may call initVisuals or update tab visibility again.
    @Inject(method = "render", at = @At("HEAD"))
    private void brbtabs$keepOriginalTabsHidden(GuiGraphics gui, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        brbtabs$hideOriginalTabs();
    }

    @Unique
    private void brbtabs$refresh(boolean resetPage) {
        if (!brbtabs$crafting() || brbtabs$tabs.isEmpty() || minecraft == null || minecraft.player == null || book == null) return;
        if (brbtabs$selected >= brbtabs$tabs.size()) brbtabs$selected = 0;

        CreativeModeTab tab = brbtabs$tabs.get(brbtabs$selected);
        Set<ResourceLocation> accepted = new HashSet<>();
        if (!brbtabs$searchAll) {
            for (ItemStack stack : tab.getDisplayItems()) {
                if (!stack.isEmpty()) accepted.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            }
        }

        // Reuse the recipe book's OWN collections. Constructing one-recipe
        // collections here broke BRB's instant-craft state, leaving its
        // RecipeButton.getOrderedRecipes() without a cached clicked recipe
        // and crashing on new ArrayList<>(null) in modded modpacks.
        // This can include other results in the same vanilla recipe group;
        // safe recipe separation needs a later dedicated implementation.
        List<RecipeCollection> entries = new ArrayList<>();
        String query = lastSearch == null ? "" : lastSearch.strip().toLowerCase(java.util.Locale.ROOT);
        for (RecipeCollection group : book.getCollection(RecipeBookCategories.CRAFTING_SEARCH)) {
            boolean matches = false;
            for (Recipe<?> recipe : group.getRecipes()) {
                ItemStack result = recipe.getResultItem(group.registryAccess());
                if (result.isEmpty() || (!brbtabs$searchAll && !accepted.contains(BuiltInRegistries.ITEM.getKey(result.getItem())))) continue;
                if (!query.isEmpty() && !result.getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains(query)
                    && !BuiltInRegistries.ITEM.getKey(result.getItem()).toString().contains(query)) continue;
                matches = true;
                break;
            }
            if (matches && (!book.isFiltering(menu) || group.hasCraftable())) {
                entries.add(group);
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
        return (width - 147) / 2 - xOffset - 49;
    }

    @Unique
    private int brbtabs$top() {
        return (height - 166) / 2 + 3;
    }

    @Unique
    private int brbtabs$tabX(int left, int index) {
        return left + (index % BRBTABS_COLUMNS) * 23;
    }

    @Unique
    private int brbtabs$tabY(int top, int index) {
        return top + 24 + (index / BRBTABS_COLUMNS) * 23;
    }

    @Unique
    private int brbtabs$bottom(int top) {
        return top + 141;
    }

    // The panel uses the actual vanilla 1.20.1 recipe-book atlas, including
    // resource-pack replacements. Nine-slice the vanilla book background to
    // keep its 1px borders and its native pixel density instead of scaling it.
    @Unique
    private void brbtabs$bookPanel(GuiGraphics gui, int x, int y) {
        final int height = 166;
        gui.blit(BRBTABS_VANILLA_BOOK, x, y, 0, 0, 8, height);
        gui.blit(BRBTABS_VANILLA_BOOK, x + 8, y, 65, 0, 31, height);
        gui.blit(BRBTABS_VANILLA_BOOK, x + 39, y, 139, 0, 8, height);
    }

    // Minecraft's default grey-beige widget bevel: no colored modern boxes.
    @Unique
    private void brbtabs$vanillaButton(GuiGraphics gui, int x, int y, int w, int h, boolean selected, boolean hovered) {
        int edgeLight = selected ? 0xFFFFFFFF : 0xFFFFFFFF;
        int edgeDark = 0xFF373737;
        int face = selected ? 0xFFC6C6C6 : hovered ? 0xFFE0E0E0 : 0xFFC6C6C6;
        gui.fill(x, y, x + w, y + h, 0xFF000000);
        gui.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
        gui.fill(x + 1, y + 1, x + w - 1, y + 2, selected ? edgeDark : edgeLight);
        gui.fill(x + 1, y + 1, x + 2, y + h - 1, selected ? edgeDark : edgeLight);
        gui.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, selected ? edgeLight : edgeDark);
        gui.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, selected ? edgeLight : edgeDark);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void brbtabs$renderTabs(GuiGraphics gui, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!brbtabs$crafting() || brbtabs$tabs.isEmpty()) return;
        int x = brbtabs$left();
        int y = brbtabs$top();

        brbtabs$bookPanel(gui, x - 1, y - 2);

        // Vanilla-style scope switch: TAB = current mod tab, ALL = all recipes.
        brbtabs$vanillaButton(gui, x + 2, y + 3, 43, 19, brbtabs$searchAll, mouseX >= x + 2 && mouseX < x + 45 && mouseY >= y + 3 && mouseY < y + 22);
        gui.drawCenteredString(minecraft.font, brbtabs$searchAll ? "ALL" : "TAB", x + 23, y + 8, 0xFF404040);

        int begin = brbtabs$page * BRBTABS_PAGE_SIZE;
        for (int i = begin; i < Math.min(begin + BRBTABS_PAGE_SIZE, brbtabs$tabs.size()); i++) {
            int offset = i - begin;
            int tx = brbtabs$tabX(x, offset);
            int ty = brbtabs$tabY(y, offset);
            boolean selected = i == brbtabs$selected && !brbtabs$searchAll;
            boolean hovered = mouseX >= tx && mouseX < tx + BRBTABS_CELL && mouseY >= ty && mouseY < ty + BRBTABS_CELL;
            brbtabs$vanillaButton(gui, tx, ty, 22, 22, selected, hovered);
            gui.renderItem(brbtabs$tabs.get(i).getIconItem(), tx + 3, ty + 3);
        }

        int bottom = brbtabs$bottom(y);
        brbtabs$vanillaButton(gui, x, bottom, 23, 18, false, mouseX >= x && mouseX < x + 23 && mouseY >= bottom && mouseY < bottom + 18);
        brbtabs$vanillaButton(gui, x + 24, bottom, 23, 18, false, mouseX >= x + 24 && mouseX < x + 47 && mouseY >= bottom && mouseY < bottom + 18);
        gui.drawCenteredString(minecraft.font, "<", x + 11, bottom + 5, brbtabs$page > 0 ? 0xFF222222 : 0xFF888888);
        gui.drawCenteredString(minecraft.font, ">", x + 35, bottom + 5,
            (brbtabs$page + 1) * BRBTABS_PAGE_SIZE < brbtabs$tabs.size() ? 0xFF222222 : 0xFF888888);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void brbtabs$click(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!brbtabs$crafting() || brbtabs$tabs.isEmpty() || button != 0) return;
        int x = brbtabs$left();
        int y = brbtabs$top();
        if (mouseX < x || mouseX >= x + 47) return;

        if (mouseY >= y && mouseY < y + 21) {
            brbtabs$searchAll = !brbtabs$searchAll;
            brbtabs$refresh(true);
            cir.setReturnValue(true);
            return;
        }

        int bottom = brbtabs$bottom(y);
        if (mouseY >= bottom && mouseY < bottom + 18) {
            if (mouseX < x + 23) brbtabs$page = Math.max(0, brbtabs$page - 1);
            else brbtabs$page = Math.min((brbtabs$tabs.size() - 1) / BRBTABS_PAGE_SIZE, brbtabs$page + 1);
            cir.setReturnValue(true);
            return;
        }

        int begin = brbtabs$page * BRBTABS_PAGE_SIZE;
        for (int i = begin; i < Math.min(begin + BRBTABS_PAGE_SIZE, brbtabs$tabs.size()); i++) {
            int tx = brbtabs$tabX(x, i - begin);
            int ty = brbtabs$tabY(y, i - begin);
            if (mouseX >= tx && mouseX < tx + BRBTABS_CELL && mouseY >= ty && mouseY < ty + BRBTABS_CELL) {
                brbtabs$selected = i;
                brbtabs$searchAll = false;
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
        if (mouseX < left || mouseX >= left + 47) return;

        if (mouseY >= top && mouseY < top + 21) {
            gui.renderTooltip(minecraft.font,
                net.minecraft.network.chat.Component.literal(brbtabs$searchAll
                    ? "Search: ALL recipes (click for selected tab)"
                    : "Search: selected Creative tab (click for ALL)"),
                mouseX, mouseY);
            return;
        }

        int begin = brbtabs$page * BRBTABS_PAGE_SIZE;
        for (int i = begin; i < Math.min(begin + BRBTABS_PAGE_SIZE, brbtabs$tabs.size()); i++) {
            int tx = brbtabs$tabX(left, i - begin);
            int ty = brbtabs$tabY(top, i - begin);
            if (mouseX >= tx && mouseX < tx + BRBTABS_CELL && mouseY >= ty && mouseY < ty + BRBTABS_CELL) {
                gui.renderTooltip(minecraft.font, brbtabs$tabs.get(i).getDisplayName(), mouseX, mouseY);
                return;
            }
        }
    }
}
