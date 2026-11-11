package gr.daldon.brbtabs.client;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only categorization of recipe *identities*, not ingredient patterns.
 * Run on the CLIENT, after creative-tab content has been populated.
 * Never use this map to authorize an output on the server.
 */
public final class CreativeRecipeIndex {
    public static final ResourceLocation UNCATEGORIZED =
        new ResourceLocation("brbtabs", "uncategorized");

    private CreativeRecipeIndex() {}

    public static Map<ResourceLocation, List<ResourceLocation>> index(
        Collection<? extends Recipe<?>> recipes, RegistryAccess registryAccess
    ) {
        Map<ResourceLocation, List<ResourceLocation>> result = new LinkedHashMap<>();
        Map<ResourceLocation, List<CreativeModeTab>> tabsByItem = new LinkedHashMap<>();

        // The built-in ordered collection also includes tabs registered by mods.
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (tabId == null || tab.getType() != CreativeModeTab.Type.CATEGORY) {
                continue;
            }

            result.put(tabId, new ArrayList<>());
            for (ItemStack displayed : tab.getDisplayItems()) {
                if (displayed.isEmpty()) continue;
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(displayed.getItem());
                tabsByItem.computeIfAbsent(itemId, k -> new ArrayList<>()).add(tab);
            }
        }

        result.put(UNCATEGORIZED, new ArrayList<>());
        for (Recipe<?> recipe : recipes) {
            ItemStack output = recipe.getResultItem(registryAccess);
            if (output.isEmpty()) continue;

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(output.getItem());
            List<CreativeModeTab> tabs = tabsByItem.get(itemId);
            if (tabs == null || tabs.isEmpty()) {
                result.get(UNCATEGORIZED).add(recipe.getId());
            } else {
                for (CreativeModeTab tab : tabs) {
                    ResourceLocation tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                    result.get(tabId).add(recipe.getId());
                }
            }
        }
        return result;
    }
}
