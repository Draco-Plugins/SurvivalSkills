package sir_draco.survivalskills.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Compares two Bukkit recipes for logical equality (ingredients + result) so the
 * recipe registrar can avoid redundant Bukkit re-registration when an identical
 * recipe already exists.
 */
public final class RecipeComparator {

    private RecipeComparator() {}

    /**
     * Compares two Bukkit recipes for logical equality (ingredients + result).
     * If types differ it returns false.
     */
    public static boolean recipesEqual(Recipe a, Recipe b) {
        if (a.equals(b))
            return true;
        if (a == null || b == null)
            return false;

        // Compare shaped recipes
        if (a instanceof ShapedRecipe sa && b instanceof ShapedRecipe sb) {
            if (!itemStacksSimilar(sa.getResult(), sb.getResult(), true))
                return false;
            String[] shapeA = sa.getShape();
            String[] shapeB = sb.getShape();
            if (shapeA.length != shapeB.length)
                return false;
            for (int i = 0; i < shapeA.length; i++)
                if (!Objects.equals(shapeA[i], shapeB[i]))
                    return false;

            // Collect characters used (excluding spaces)
            Set<Character> chars = new HashSet<>();
            for (String line : shapeA)
                for (char c : line.toCharArray())
                        if (c != ' ')
                                chars.add(c);

            Map<Character, ItemStack> ingA = sa.getIngredientMap();
            Map<Character, ItemStack> ingB = sb.getIngredientMap();
            Map<Character, RecipeChoice> choiceA = sa.getChoiceMap();
            Map<Character, RecipeChoice> choiceB = sb.getChoiceMap();

            for (char c : chars) {
                // Prefer choice maps (ExactChoice / MaterialChoice) if present
                RecipeChoice ca = choiceA.get(c);
                RecipeChoice cb = choiceB.get(c);
                if (ca != null || cb != null) {
                    if (!recipeChoicesEqual(ca, cb))
                        return false;
                    continue;
                }
                ItemStack ia = ingA.get(c);
                ItemStack ib = ingB.get(c);
                if (!itemStacksSimilar(ia, ib, false))
                    return false;
            }
            return true;
        }

        // Compare shapeless recipes
        if (a instanceof ShapelessRecipe sa && b instanceof ShapelessRecipe sb) {
            if (!itemStacksSimilar(sa.getResult(), sb.getResult(), true))
                return false;
            List<ItemStack> listA = sa.getIngredientList();
            List<ItemStack> listB = sb.getIngredientList();
            if (listA.size() != listB.size())
                return false;

            // Multiset compare ignoring order
            List<ItemStack> remaining = new ArrayList<>(listB);
            outer: for (ItemStack ia : listA) {
                for (int i = 0; i < remaining.size(); i++) {
                    if (itemStacksSimilar(ia, remaining.get(i), false)) {
                        remaining.remove(i);
                        continue outer;
                    }
                }
                return false; // no match for ingredient
            }
            return remaining.isEmpty();
        }

        // Different recipe types (or unhandled types) -> treat as different so
        // replacement occurs
        return false;
    }

    private static boolean recipeChoicesEqual(RecipeChoice a, RecipeChoice b) {
        if (a.equals(b))
            return true;
        if (a == null || b == null)
            return false;
        if (!a.getClass().equals(b.getClass()))
            return false;

        // ExactChoice
        if (a instanceof RecipeChoice.ExactChoice ea && b instanceof RecipeChoice.ExactChoice eb) {
            List<ItemStack> la = ea.getChoices();
            List<ItemStack> lb = eb.getChoices();
            if (la.size() != lb.size())
                return false;
            for (int i = 0; i < la.size(); i++)
                if (!itemStacksSimilar(la.get(i), lb.get(i), false))
                        return false;
            return true;
        }

        // MaterialChoice
        if (a instanceof RecipeChoice.MaterialChoice ma && b instanceof RecipeChoice.MaterialChoice mb) {
            return new HashSet<>(ma.getChoices()).equals(new HashSet<>(mb.getChoices()));
        }

        // Fallback: not equal (covers new choice types)
        return false;
    }

    private static boolean itemStacksSimilar(ItemStack a, ItemStack b, boolean checkAmount) {
        if (a.equals(b))
            return true;
        if (a == null || b == null)
            return false;
        if (!a.getType().equals(b.getType()))
            return false;
        ItemMeta aMeta = a.getItemMeta();
        ItemMeta bMeta = b.getItemMeta();
        if (aMeta == null || bMeta == null)
            return false;
        if (!aMeta.equals(bMeta))
            return false;
        return !checkAmount || a.getAmount() == b.getAmount();
    }
}