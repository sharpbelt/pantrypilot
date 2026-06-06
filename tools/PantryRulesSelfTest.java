package app.pantrypilot.app;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PantryRulesSelfTest {
    private static int passed = 0;

    public static void main(String[] args) {
        check("blank name rejected by normalization", PantryRules.normalizeName("   ").isEmpty());
        check("whitespace collapses", PantryRules.normalizeName("  greek    yogurt  ").equals("greek yogurt"));
        check("name length capped", PantryRules.normalizeName(repeat("a", 120)).length() == PantryRules.MAX_NAME_LENGTH);
        check("blank quantity defaults", PantryRules.normalizeQuantity("  ").equals("1"));
        check("quantity length capped", PantryRules.normalizeQuantity(repeat("x", 80)).length() == PantryRules.MAX_QUANTITY_LENGTH);
        check("unknown plan falls back to free", PantryRules.normalizePlan("Enterprise").equals(PantryRules.PLAN_FREE));
        check("plus plan normalization", PantryRules.normalizePlan(" plus ").equals(PantryRules.PLAN_PLUS));
        check("pro outranks plus", PantryRules.isAtLeast(PantryRules.PLAN_PRO, PantryRules.PLAN_PLUS));
        check("free does not outrank plus", !PantryRules.isAtLeast(PantryRules.PLAN_FREE, PantryRules.PLAN_PLUS));
        check("free pantry limit", PantryRules.pantryLimit(PantryRules.PLAN_FREE) == 12);
        check("plus pantry limit", PantryRules.pantryLimit(PantryRules.PLAN_PLUS) == 150);
        check("pro grocery limit", PantryRules.groceryLimit(PantryRules.PLAN_PRO) == 300);
        check("free pantry limit blocks at cap", !PantryRules.canAddPantryItem(PantryRules.PLAN_FREE, 12));
        check("free pantry allows below cap", PantryRules.canAddPantryItem(PantryRules.PLAN_FREE, 11));
        check("free scanner locked", !PantryRules.canUsePhotoScanner(PantryRules.PLAN_FREE));
        check("plus scanner unlocked", PantryRules.canUsePhotoScanner(PantryRules.PLAN_PLUS));
        check("free sharing locked", !PantryRules.canShare(PantryRules.PLAN_FREE));
        check("plus sharing unlocked", PantryRules.canShare(PantryRules.PLAN_PLUS));
        check("meal extras pro only", PantryRules.canUseMealExtras(PantryRules.PLAN_PRO) && !PantryRules.canUseMealExtras(PantryRules.PLAN_PLUS));
        check("free meal idea preview limit", PantryRules.mealIdeaLimit(PantryRules.PLAN_FREE) == 2);
        check("plus meal idea full limit", PantryRules.mealIdeaLimit(PantryRules.PLAN_PLUS) == 4);
        check("plus product id", PantryRules.productIdForPlan(PantryRules.PLAN_PLUS).equals(PantryRules.PLUS_PRODUCT_ID));
        check("pro product id", PantryRules.productIdForPlan(PantryRules.PLAN_PRO).equals(PantryRules.PRO_PRODUCT_ID));
        check("remove ads product id", PantryRules.REMOVE_ADS_PRODUCT_ID.equals("pantrypilot_remove_ads_one_time"));
        check("free plan does not remove ads by itself", !PantryRules.planRemovesAds(PantryRules.PLAN_FREE));
        check("plus and pro remove ads", PantryRules.planRemovesAds(PantryRules.PLAN_PLUS) && PantryRules.planRemovesAds(PantryRules.PLAN_PRO));
        check("empty date allowed", PantryRules.validDate(""));
        check("compact date normalizes", PantryRules.normalizeDateInput("20261231").equals("2026-12-31"));
        check("slash date normalizes", PantryRules.normalizeDateInput("2026/12/31").equals("2026-12-31"));
        check("compact date accepted", PantryRules.validDate("20261231"));
        check("valid leap date accepted", PantryRules.validDate("2028-02-29"));
        check("invalid compact calendar date rejected", !PantryRules.validDate("20260231"));
        check("invalid calendar date rejected", !PantryRules.validDate("2026-02-31"));
        check("natural date rejected", !PantryRules.validDate("tomorrow"));
        check("shopping duplicate detects case and spaces", PantryRules.containsShopName(Arrays.asList("Greek yogurt"), "  greek   YOGURT "));
        check("shopping duplicate ignores empty", !PantryRules.containsShopName(Arrays.asList("Eggs"), " "));
        check("grocery group produce", PantryRules.groceryGroup("baby spinach").equals("Produce"));
        check("grocery group protein", PantryRules.groceryGroup("black beans").equals("Protein"));
        check("grocery group dairy", PantryRules.groceryGroup("Greek yogurt").equals("Dairy"));
        check("scanner category guess", PantryRules.guessCategory("eggs").equals("Protein"));
        PantryRules.ScanDraft draft = PantryRules.parsePackagingText("Greek Yogurt\nNet Wt 500g\nBest Before 2026-12-31\nKeep refrigerated");
        check("package OCR parser name", draft.name.equals("Greek Yogurt"));
        check("package OCR parser quantity", draft.quantity.equals("500g"));
        check("package OCR parser expiry", draft.expiry.equals("2026-12-31"));
        check("package OCR parser refrigerated location", draft.location.equals("Fridge"));
        check("package OCR parser category", draft.category.equals("Dairy"));
        PantryRules.ScanDraft ukDraft = PantryRules.parsePackagingText("Black Beans\n400g\nUse By 31/12/2026");
        check("package OCR parser UK date", ukDraft.expiry.equals("2026-12-31"));
        check("package OCR parser shelf-stable location", ukDraft.location.equals("Pantry"));
        PantryRules.ScanDraft frozenDraft = PantryRules.parsePackagingText("Garden Peas\n1 kg\nBest Before 2027-01-10\nKeep frozen");
        check("package OCR parser frozen location", frozenDraft.location.equals("Freezer"));
        PantryRules.ScanDraft chilledMeat = PantryRules.parsePackagingText("Chicken Breast\n500g\nPreviously frozen\nKeep refrigerated");
        check("previously frozen chilled item stays fridge", chilledMeat.location.equals("Fridge"));
        check("location fallback for dairy name", PantryRules.guessLocation("Greek yogurt", "Dairy").equals("Fridge"));
        List<String> ideas = PantryRules.mealIdeas(
                Arrays.asList("black beans", "spinach", "rice"),
                Arrays.asList("Protein", "Vegetable", "Grain"));
        check("meal ideas generated", ideas.size() == 4);
        String firstIdea = ideas.get(0).toLowerCase();
        check("meal idea uses pantry input", firstIdea.contains("black beans") && firstIdea.contains("rice") && firstIdea.contains("spinach"));
        List<String> fallback = PantryRules.mealIdeas(Collections.emptyList(), Collections.emptyList());
        check("meal ideas fallback when pantry empty", fallback.get(0).toLowerCase().contains("beans"));

        System.out.println("PantryRulesSelfTest passed " + passed + " checks.");
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError(name);
        }
        passed++;
    }

    private static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) builder.append(value);
        return builder.toString();
    }
}
