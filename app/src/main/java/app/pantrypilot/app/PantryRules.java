package app.pantrypilot.app;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PantryRules {
    static final int MAX_NAME_LENGTH = 80;
    static final int MAX_QUANTITY_LENGTH = 40;
    static final String PLAN_FREE = "Free";
    static final String PLAN_PLUS = "Plus";
    static final String PLAN_PRO = "Pro";
    static final String REMOVE_ADS_PRODUCT_ID = "pantrypilot_remove_ads_one_time";
    static final String PLUS_PRODUCT_ID = "pantrypilot_plus_one_time";
    static final String PRO_PRODUCT_ID = "pantrypilot_pro_one_time";
    private static final Pattern ISO_DATE = Pattern.compile("\\b(20\\d{2})[-/.](0?[1-9]|1[0-2])[-/.](0?[1-9]|[12]\\d|3[01])\\b");
    private static final Pattern UK_DATE = Pattern.compile("\\b(0?[1-9]|[12]\\d|3[01])[-/.](0?[1-9]|1[0-2])[-/.](20\\d{2})\\b");
    private static final Pattern QUANTITY = Pattern.compile("\\b(\\d+(?:\\.\\d+)?\\s?(?:g|kg|ml|l|litre|litres|oz|lb|lbs|pack|packs|can|cans|bottle|bottles|pcs|pieces))\\b", Pattern.CASE_INSENSITIVE);

    private PantryRules() {
    }

    static String normalizeName(String raw) {
        return limit(collapseWhitespace(raw), MAX_NAME_LENGTH);
    }

    static String normalizeQuantity(String raw) {
        String value = limit(collapseWhitespace(raw), MAX_QUANTITY_LENGTH);
        return value.isEmpty() ? "1" : value;
    }

    static String normalizePlan(String raw) {
        String value = collapseWhitespace(raw);
        if (PLAN_PRO.equalsIgnoreCase(value)) return PLAN_PRO;
        if (PLAN_PLUS.equalsIgnoreCase(value)) return PLAN_PLUS;
        return PLAN_FREE;
    }

    static int planRank(String plan) {
        String normalized = normalizePlan(plan);
        if (PLAN_PRO.equals(normalized)) return 2;
        if (PLAN_PLUS.equals(normalized)) return 1;
        return 0;
    }

    static boolean isAtLeast(String currentPlan, String requiredPlan) {
        return planRank(currentPlan) >= planRank(requiredPlan);
    }

    static int pantryLimit(String plan) {
        if (PLAN_PRO.equals(normalizePlan(plan))) return 500;
        if (PLAN_PLUS.equals(normalizePlan(plan))) return 150;
        return 12;
    }

    static int groceryLimit(String plan) {
        if (PLAN_PRO.equals(normalizePlan(plan))) return 300;
        if (PLAN_PLUS.equals(normalizePlan(plan))) return 100;
        return 12;
    }

    static int mealIdeaLimit(String plan) {
        return isAtLeast(plan, PLAN_PLUS) ? 4 : 2;
    }

    static boolean canUsePhotoScanner(String plan) {
        return isAtLeast(plan, PLAN_PLUS);
    }

    static boolean canShare(String plan) {
        return isAtLeast(plan, PLAN_PLUS);
    }

    static boolean canUseMealExtras(String plan) {
        return isAtLeast(plan, PLAN_PRO);
    }

    static boolean canAddPantryItem(String plan, int currentCount) {
        return currentCount < pantryLimit(plan);
    }

    static boolean canAddGroceryItem(String plan, int currentCount) {
        return currentCount < groceryLimit(plan);
    }

    static String productIdForPlan(String plan) {
        String normalized = normalizePlan(plan);
        if (PLAN_PRO.equals(normalized)) return PRO_PRODUCT_ID;
        if (PLAN_PLUS.equals(normalized)) return PLUS_PRODUCT_ID;
        return "";
    }

    static boolean planRemovesAds(String plan) {
        return isAtLeast(plan, PLAN_PLUS);
    }

    static String planSummary(String plan) {
        String normalized = normalizePlan(plan);
        if (PLAN_PRO.equals(normalized)) {
            return "Pro: 500 pantry items, 300 grocery items, scanner, sharing, meal extras, and no ads.";
        }
        if (PLAN_PLUS.equals(normalized)) {
            return "Plus: 150 pantry items, 100 grocery items, scanner, sharing, full meal ideas, and no ads.";
        }
        return "Free: 12 pantry items, 12 grocery items, sample scanner preview, 2 meal ideas, and banner ads.";
    }

    static boolean validDate(String date) {
        String normalized = normalizeDateInput(date);
        if (normalized.isEmpty()) return true;
        try {
            LocalDate.parse(normalized);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    static String normalizeDateInput(String raw) {
        String value = collapseWhitespace(raw);
        if (value.isEmpty()) return "";
        value = value.replaceAll("\\s+", "").replace('/', '-').replace('.', '-');
        if (value.matches("\\d{8}")) {
            value = value.substring(0, 4) + "-" + value.substring(4, 6) + "-" + value.substring(6, 8);
        }
        return value;
    }

    static String groceryGroup(String name) {
        String n = normalizeName(name).toLowerCase(Locale.US);
        if (n.contains("apple") || n.contains("banana") || n.contains("spinach") || n.contains("onion") || n.contains("garlic") || n.contains("veg") || n.contains("lettuce")) return "Produce";
        if (n.contains("egg") || n.contains("chicken") || n.contains("beef") || n.contains("fish") || n.contains("bean") || n.contains("tofu")) return "Protein";
        if (n.contains("milk") || n.contains("yogurt") || n.contains("cheese") || n.contains("cream")) return "Dairy";
        if (n.contains("rice") || n.contains("pasta") || n.contains("flour") || n.contains("oil") || n.contains("sauce") || n.contains("bread")) return "Pantry";
        if (n.contains("foil") || n.contains("soap") || n.contains("bag") || n.contains("paper")) return "Household";
        return "Other";
    }

    static String guessCategory(String name) {
        String group = groceryGroup(name);
        if (group.equals("Produce")) return "Vegetable";
        if (group.equals("Protein")) return "Protein";
        if (group.equals("Dairy")) return "Dairy";
        if (group.equals("Pantry")) return "Grain";
        return "Other";
    }

    static ScanDraft parsePackagingText(String rawText) {
        String normalized = rawText == null ? "" : rawText.replace('\r', '\n');
        String name = "";
        for (String line : normalized.split("\\n")) {
            String candidate = normalizeName(line.replaceAll("(?i)^(product|item|name)\\s*[:\\-]\\s*", ""));
            if (candidate.isEmpty() || looksLikeMetadata(candidate)) continue;
            name = candidate;
            break;
        }

        String expiry = "";
        Matcher iso = ISO_DATE.matcher(normalized);
        if (iso.find()) {
            expiry = padDate(iso.group(1), iso.group(2), iso.group(3));
        } else {
            Matcher uk = UK_DATE.matcher(normalized);
            if (uk.find()) expiry = padDate(uk.group(3), uk.group(2), uk.group(1));
        }
        if (!validDate(expiry)) expiry = "";

        String quantity = "";
        Matcher qty = QUANTITY.matcher(normalized);
        if (qty.find()) quantity = normalizeQuantity(qty.group(1));

        String category = guessCategory(name + " " + normalized);
        String location = guessLocation(name + " " + normalized, category);
        return new ScanDraft(name, quantity.isEmpty() ? "1" : quantity, expiry, location, category);
    }

    static String guessLocation(String rawText, String category) {
        String text = normalizeName(rawText).toLowerCase(Locale.US);
        boolean frozenWordMeansStorage = text.contains("frozen") && !containsAny(text, "previously frozen", "not frozen");
        if (containsAny(text, "keep frozen", "store frozen", "stored frozen", "freezer") || frozenWordMeansStorage) return "Freezer";
        if (containsAny(text, "keep refrigerated", "store refrigerated", "refrigerate", "refrigerated", "chilled", "keep chilled")) return "Fridge";
        if (containsAny(text, "cool dry place", "cool, dry place", "room temperature", "ambient", "shelf stable")) return "Pantry";
        if (containsAny(text, "ice cream", "frozen peas", "frozen veg")) return "Freezer";
        if (category.equals("Dairy")) return "Fridge";
        if (containsAny(text, "milk", "yogurt", "cheese", "cream", "egg", "chicken", "beef", "fish", "spinach", "lettuce")) return "Fridge";
        return "Pantry";
    }

    static final class ScanDraft {
        final String name;
        final String quantity;
        final String expiry;
        final String location;
        final String category;

        ScanDraft(String name, String quantity, String expiry, String location, String category) {
            this.name = name;
            this.quantity = quantity;
            this.expiry = expiry;
            this.location = location;
            this.category = category;
        }
    }

    static boolean containsShopName(List<String> shoppingNames, String rawName) {
        String name = normalizeName(rawName);
        if (name.isEmpty()) return false;
        for (String existing : shoppingNames) {
            if (normalizeName(existing).equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    static List<String> mealIdeas(List<String> names, List<String> categories) {
        Map<String, List<String>> byCat = new HashMap<>();
        for (int i = 0; i < names.size() && i < categories.size(); i++) {
            String name = normalizeName(names.get(i));
            String category = normalizeName(categories.get(i));
            if (!name.isEmpty() && !category.isEmpty()) {
                byCat.computeIfAbsent(category, k -> new ArrayList<>()).add(name);
            }
        }
        List<String> ideas = new ArrayList<>();
        String protein = first(byCat, "Protein", "beans");
        String veg = first(byCat, "Vegetable", "mixed veg");
        String grain = first(byCat, "Grain", "rice");
        String dairy = first(byCat, "Dairy", "cheese");
        String sauce = first(byCat, "Sauce", "seasoning");
        ideas.add(cap(protein) + " bowl with " + grain + " and " + veg);
        ideas.add(cap(veg) + " and " + dairy + " quick bake");
        ideas.add(cap(protein) + " wraps with " + sauce);
        ideas.add("Clear-out soup using " + veg + ", " + protein + ", and pantry spices");
        return ideas;
    }

    private static String first(Map<String, List<String>> map, String key, String fallback) {
        List<String> list = map.get(key);
        return list == null || list.isEmpty() ? fallback : list.get(0);
    }

    private static String collapseWhitespace(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("\\s+", " ");
    }

    private static boolean looksLikeMetadata(String value) {
        String lower = value.toLowerCase(Locale.US);
        return lower.contains("expiry") || lower.contains("expires") || lower.contains("best before") ||
                lower.contains("use by") || lower.contains("net wt") || lower.contains("weight") ||
                lower.contains("barcode") || lower.contains("nutrition") || lower.matches(".*\\d{3,}.*");
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    private static String padDate(String year, String month, String day) {
        return year + "-" + (month.length() == 1 ? "0" + month : month) + "-" + (day.length() == 1 ? "0" + day : day);
    }

    private static String limit(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, max).trim();
    }

    private static String cap(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }
}
